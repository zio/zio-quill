package io.getquill

import io.getquill.ast._
import io.getquill.context.CanOutputClause
import io.getquill.context.sql.idiom._
import io.getquill.context.sql.norm.AddDropToNestedOrderBy
import io.getquill.context.sql.{ FlattenSqlQuery, SqlQuery }
import io.getquill.idiom.StatementInterpolator._
import io.getquill.idiom.{ Statement, StringToken, Token }
import io.getquill.norm.EqualityBehavior
import io.getquill.norm.EqualityBehavior.NonAnsiEquality
import io.getquill.util.Messages.fail

trait SQLServerDialect
  extends SqlIdiom
  with QuestionMarkBindVariables
  with ConcatSupport
  with CanOutputClause {

  override def querifyAst(ast: Ast) = AddDropToNestedOrderBy(SqlQuery(ast))

  override def emptySetContainsToken(field: Token) = StringToken("1 <> 1")

  override def prepareForProbing(string: String) = string

  // SQL-Server can potentially disable ANSI-null via `SET ANSI_NULLS OFF`. Force more strict checking here
  // for the sake of consistency with the other contexts.
  override def equalityBehavior: EqualityBehavior = NonAnsiEquality

  override protected def limitOffsetToken(query: Statement)(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy) =
    Tokenizer[(Option[Ast], Option[Ast])] {
      case (Some(limit), None)         => stmt"TOP ${limit.token} $query"
      case (Some(limit), Some(offset)) => stmt"$query OFFSET ${offset.token} ROWS FETCH FIRST ${limit.token} ROWS ONLY"
      case (None, Some(offset))        => stmt"$query OFFSET ${offset.token} ROWS"
      case other                       => super.limitOffsetToken(query).token(other)
    }

  override implicit def sqlQueryTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[SqlQuery] =
    Tokenizer[SqlQuery] {
      case flatten: FlattenSqlQuery if flatten.orderBy.isEmpty && flatten.offset.nonEmpty =>
        fail(s"SQLServer does not support OFFSET without ORDER BY")
      case other => super.sqlQueryTokenizer.token(other)
    }

  override implicit def operationTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[Operation] =
    Tokenizer[Operation] {
      case BinaryOperation(a, StringOperator.`+`, b) => stmt"${scopedTokenizer(a)} + ${scopedTokenizer(b)}"
      case other                                     => super.operationTokenizer.token(other)
    }

  override implicit def valueTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[Value] =
    Tokenizer[Value] {
      case Constant(b: Boolean) => StringToken(if (b) "1=1" else "1=0")
      case other                => super.valueTokenizer.token(other)
    }
}

object SQLServerDialect extends SQLServerDialect
