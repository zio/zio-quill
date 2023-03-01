package io.getquill

import io.getquill.ast._
import io.getquill.context.{ CanInsertReturningWithMultiValues, CanInsertWithMultiValues, CanOutputClause }
import io.getquill.context.sql.idiom.SqlIdiom.ActionTableAliasBehavior
import io.getquill.context.sql.idiom._
import io.getquill.context.sql.norm.AddDropToNestedOrderBy
import io.getquill.context.sql.{ FlattenSqlQuery, SqlQuery, SqlQueryApply }
import io.getquill.idiom.StatementInterpolator._
import io.getquill.idiom.{ Statement, StringToken, Token }
import io.getquill.norm.EqualityBehavior.NonAnsiEquality
import io.getquill.norm.EqualityBehavior
import io.getquill.sql.idiom.BooleanLiteralSupport
import io.getquill.util.Messages.fail
import io.getquill.util.TraceConfig

trait SQLServerDialect
  extends SqlIdiom
  with QuestionMarkBindVariables
  with ConcatSupport
  with CanOutputClause
  with BooleanLiteralSupport
  with CanInsertWithMultiValues
  with CanInsertReturningWithMultiValues {

  override def useActionTableAliasAs: ActionTableAliasBehavior = ActionTableAliasBehavior.Hide

  override def querifyAst(ast: Ast, idiomContext: TraceConfig) = AddDropToNestedOrderBy(new SqlQueryApply(idiomContext)(ast))

  override def emptySetContainsToken(field: Token) = StringToken("1 <> 1")

  override def prepareForProbing(string: String) = string

  // SQL-Server can potentially disable ANSI-null via `SET ANSI_NULLS OFF`. Force more strict checking here
  // for the sake of consistency with the other contexts.
  override def equalityBehavior: EqualityBehavior = NonAnsiEquality

  override protected def limitOffsetToken(query: Statement)(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy) =
    Tokenizer[(Option[Ast], Option[Ast])] {
      case (Some(limit), None)         => stmt"TOP (${limit.token}) $query"
      case (Some(limit), Some(offset)) => stmt"$query OFFSET ${offset.token} ROWS FETCH FIRST ${limit.token} ROWS ONLY"
      case (None, Some(offset))        => stmt"$query OFFSET ${offset.token} ROWS"
      case other                       => super.limitOffsetToken(query).token(other)
    }

  override implicit def sqlQueryTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy, idiomContext: IdiomContext): Tokenizer[SqlQuery] =
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

  override protected def actionTokenizer(insertEntityTokenizer: Tokenizer[Entity])(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy, idiomContext: IdiomContext): Tokenizer[ast.Action] =
    Tokenizer[ast.Action] {
      // Update(Filter(...)) and Delete(Filter(...)) usually cause a table alias i.e. `UPDATE People <alias> SET ... WHERE ...` or `DELETE FROM People <alias> WHERE ...`
      // since the alias is used in the WHERE clause. This functionality removes that because SQLServer doesn't support aliasing in actions.
      case Update(Filter(table: Entity, x, where), assignments) =>
        stmt"UPDATE ${table.token} SET ${assignments.token} WHERE ${where.token}"
      case Delete(Filter(table: Entity, x, where)) =>
        stmt"DELETE FROM ${table.token} WHERE ${where.token}"
      case other => super.actionTokenizer(insertEntityTokenizer).token(other)
    }
}

object SQLServerDialect extends SQLServerDialect
