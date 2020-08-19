package io.getquill.context.spark

import io.getquill.NamingStrategy
import io.getquill.ast.{ Ast, BinaryOperation, CaseClass, Constant, ExternalIdent, Ident, Operation, Property, Query, StringOperator, Tuple, Value }
import io.getquill.context.spark.norm.EscapeQuestionMarks
import io.getquill.context.sql.{ SqlQuery }
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.context.sql.norm.SqlNormalize
import io.getquill.idiom.StatementInterpolator._
import io.getquill.idiom.Token
import io.getquill.util.Messages.trace
import io.getquill.context.CannotReturn
import io.getquill.quat.Quat

class SparkDialect extends SparkIdiom

trait SparkIdiom extends SqlIdiom with CannotReturn { self =>

  def parentTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy) = super.sqlQueryTokenizer

  def liftingPlaceholder(index: Int): String = "?"

  override def prepareForProbing(string: String) = string

  override implicit def externalIdentTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[ExternalIdent] = super.externalIdentTokenizer

  override def translate(ast: Ast)(implicit naming: NamingStrategy) = {
    val normalizedAst = EscapeQuestionMarks(SqlNormalize(ast))

    implicit val tokernizer = defaultTokenizer

    val token =
      normalizedAst match {
        case q: Query =>
          val sql = SqlQuery(q)
          trace("sql")(sql)
          val expanded = SimpleNestedExpansion(sql)
          trace("expanded sql")(expanded)
          val tokenized = expanded.token
          trace("tokenized sql")(tokenized)
          tokenized
        case other =>
          other.token
      }

    (normalizedAst, stmt"$token")
  }

  override def concatFunction = "explode"

  override implicit def identTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[Ident] = Tokenizer[Ident] {
    case id @ Ident(name, Quat.Product(fields)) =>
      stmt"struct(${fields.map { case (field, subQuat) => (Property(id, field): Ast) }.toList.token})"
    // Situations where a single ident arise with is a Quat.Value typically only happen when an operation yields a single SelectValue
    // e.g. a concatMap (or aggregation?)
    case Ident(name, Quat.Value) =>
      stmt"${name.token}.single"
    case Ident(name, _) =>
      stmt"${name.token}"
  }
  override implicit def propertyTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[Property] = {
    def path(ast: Ast): Token =
      ast match {
        case Ident(name, _) => name.token
        case Property.Opinionated(a, b, renameable, _) =>
          stmt"${path(a)}.${renameable.fixedOr(b.token)(strategy.column(b).token)}"
        case other =>
          other.token
      }
    Tokenizer[Property] {
      case p => path(p).token
    }
  }

  override implicit def operationTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[Operation] = Tokenizer[Operation] {
    case BinaryOperation(a, StringOperator.`+`, b) => stmt"concat(${a.token}, ${b.token})"
    case op                                        => super.operationTokenizer.token(op)
  }

  override implicit def valueTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[Value] = Tokenizer[Value] {
    case Constant(v: String) => stmt"'${v.replaceAll("""[\\']""", """\\$0""").token}'"
    case Tuple(values)       => stmt"struct(${values.zipWithIndex.map { case (value, index) => stmt"${value.token} AS _${(index + 1 + "").token}" }.token})"
    case CaseClass(values)   => stmt"struct(${values.map { case (name, value) => stmt"${value.token} AS ${name.token}" }.token})"
    case other               => super.valueTokenizer.token(other)
  }

  override protected def tokenizeGroupBy(values: Ast)(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Token =
    values match {
      case Tuple(items) => items.mkStmt()
      case values       => values.token
    }
}

object SparkDialect extends SparkDialect

