package io.getquill.context.spark

import io.getquill.NamingStrategy
import io.getquill.ast.{ Ast, BinaryOperation, CaseClass, Constant, ExternalIdent, Ident, Operation, Property, Query, StringOperator, Tuple, Value }
import io.getquill.context.spark.norm.EscapeQuestionMarks
import io.getquill.context.sql.{ FlattenSqlQuery, SelectValue, SetOperationSqlQuery, SqlQuery, UnaryOperationSqlQuery }
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
          trace("tokenized sql")(tokenized.toString)
          tokenized
        case other =>
          other.token
      }

    (normalizedAst, stmt"$token")
  }

  override def concatFunction = "explode"

  override implicit def identTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[Ident] = Tokenizer[Ident] {
    case id @ Ident(name, q @ Quat.Product(fields)) if (q.tpe == Quat.Product.Type.Concrete) =>
      stmt"struct(${fields.map { case (field, subQuat) => (Property(id, field): Ast) }.toList.token})"
    case id @ Ident(name, q: Quat.Product) if (q.tpe == Quat.Product.Type.Abstract) =>
      stmt"struct(${name.token}.*)"
    // Situations where a single ident arise with is a Quat.Value typically only happen when an operation yields a single SelectValue
    // e.g. a concatMap (or aggregation?)
    case Ident(name, Quat.Value) =>
      stmt"${name.token}.single"
    case Ident(name, _) =>
      stmt"${name.token}"
  }

  class SparkFlattenSqlQueryTokenizerHelper(q: FlattenSqlQuery)(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy)
    extends FlattenSqlQueryTokenizerHelper(q)(astTokenizer, strategy) {

    override def selectTokenizer: Token =
      q.select match {
        case Nil => stmt"*"
        // If we have encountered a situation where a abstract ident is on the top level and the only select field, (i.e. select.length == 1)
        // it is a strange situation that needs special handling.
        // We cannot expand the fields of the ident because there might be fields that we don't know about. We also can't just pass
        // the ident along since in the SparkDialect it will be rendered as Ident("a") => struct(a.*) which doesn't work in a top-level select
        // (i.e. you can't do "select struct(a.*) from ..." since on the top level the fields must be expanded.)
        // The only good option that I have thought of so far, is to expand the Ident in the SparkDialect directly
        // in the FlattenSqlTokenizer with special handling for length=1 selects
        case List(SelectValue(Ident(a, q @ Quat.Product(_)), _, _)) if (q.tpe == Quat.Product.Type.Abstract) =>
          stmt"${a.token}.*"
        case _ => q.select.token
      }

  }

  override implicit def sqlQueryTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[SqlQuery] = Tokenizer[SqlQuery] {
    case q: FlattenSqlQuery =>
      new SparkFlattenSqlQueryTokenizerHelper(q).apply
    case SetOperationSqlQuery(a, op, b) =>
      stmt"(${a.token}) ${op.token} (${b.token})"
    case UnaryOperationSqlQuery(op, q) =>
      stmt"SELECT ${op.token} (${q.token})"
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
    case Constant(v: String, _) => stmt"'${v.replaceAll("""[\\']""", """\\$0""").token}'"
    case Tuple(values)          => stmt"struct(${values.zipWithIndex.map { case (value, index) => stmt"${value.token} AS _${(index + 1 + "").token}" }.token})"
    case CaseClass(values)      => stmt"struct(${values.map { case (name, value) => stmt"${value.token} AS ${name.token}" }.token})"
    case other                  => super.valueTokenizer.token(other)
  }

  override protected def tokenizeGroupBy(values: Ast)(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Token =
    values match {
      case Tuple(items) => items.mkStmt()
      case values       => values.token
    }
}

object SparkDialect extends SparkDialect

