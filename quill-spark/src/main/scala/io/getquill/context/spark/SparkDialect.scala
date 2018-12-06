package io.getquill.context.spark

import io.getquill.NamingStrategy
import io.getquill.ast.Ast
import io.getquill.ast.BinaryOperation
import io.getquill.ast.Ident
import io.getquill.ast.Operation
import io.getquill.ast.Property
import io.getquill.ast.Query
import io.getquill.ast.StringOperator
import io.getquill.ast.Tuple
import io.getquill.ast.Value
import io.getquill.ast.CaseClass
import io.getquill.context.spark.norm.{ EscapeQuestionMarks, ExpandEntityIds }
import io.getquill.context.sql.SqlQuery
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.context.sql.norm.SqlNormalize
import io.getquill.idiom.StatementInterpolator._
import io.getquill.idiom.Token
import io.getquill.util.Messages.trace
import io.getquill.ast.Constant

class SparkDialect extends SqlIdiom {

  def liftingPlaceholder(index: Int): String = "?"

  override def prepareForProbing(string: String) = string

  override def translate(ast: Ast)(implicit naming: NamingStrategy) = {
    val normalizedAst = EscapeQuestionMarks(SqlNormalize(ast))

    implicit val tokernizer = defaultTokenizer

    val token =
      normalizedAst match {
        case q: Query =>
          val sql = SqlQuery(q)
          trace("sql")(sql)
          val expanded = ExpandEntityIds(sql)
          trace("expanded sql")(expanded)
          expanded.token
        case other =>
          other.token
      }

    (normalizedAst, stmt"$token")
  }

  override def concatFunction = "explode"

  override implicit def identTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[Ident] = Tokenizer[Ident] {
    case Ident(name) => stmt"${name.token}._1"
  }

  override implicit def sqlQueryTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[SqlQuery] = Tokenizer[SqlQuery] {
    case q => super.sqlQueryTokenizer.token(AliasNestedQueryColumns(q))
  }

  override implicit def propertyTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[Property] = {
    def path(ast: Ast): Token =
      ast match {
        case Ident(name) => name.token
        case Property(a, b) =>
          stmt"${path(a)}.${strategy.column(b).token}"
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
    case Tuple(values)       => stmt"(${values.token})"
    case CaseClass(values)   => stmt"${values.map({ case (prop, value) => stmt"${value.token} ${prop.token}".token }).token}"
    case other               => super.valueTokenizer.token(other)
  }

  override protected def tokenizeGroupBy(values: Ast)(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Token =
    values match {
      case Tuple(items) => items.mkStmt()
      case values       => values.token
    }
}

object SparkDialect extends SparkDialect

