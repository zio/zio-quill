package io.getquill

import io.getquill.idiom.StatementInterpolator._
import io.getquill.context.sql.idiom._
import io.getquill.idiom.StatementInterpolator.Tokenizer
import io.getquill.idiom.{ StringToken, Token }
import io.getquill.ast._
import io.getquill.context.CanReturnField
import io.getquill.context.sql.OrderByCriteria

trait SqliteDialect
  extends SqlIdiom
  with QuestionMarkBindVariables
  with NoConcatSupport
  with OnConflictSupport
  with CanReturnField {

  override def emptySetContainsToken(field: Token) = StringToken("0")

  override def prepareForProbing(string: String) = s"sqlite3_prepare_v2($string)"

  override def astTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[Ast] =
    Tokenizer[Ast] {
      case c: OnConflict => conflictTokenizer.token(c)
      case ast           => super.astTokenizer.token(ast)
    }

  private[this] val omittedNullsOrdering = stmt"omitted (not supported by sqlite)"
  private[this] val omittedNullsFirst = stmt"/* NULLS FIRST $omittedNullsOrdering */"
  private[this] val omittedNullsLast = stmt"/* NULLS LAST $omittedNullsOrdering */"

  override implicit def orderByCriteriaTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[OrderByCriteria] = Tokenizer[OrderByCriteria] {
    case OrderByCriteria(ast, Asc) =>
      stmt"${scopedTokenizer(ast)} ASC"
    case OrderByCriteria(ast, Desc) =>
      stmt"${scopedTokenizer(ast)} DESC"
    case OrderByCriteria(ast, AscNullsFirst) =>
      stmt"${scopedTokenizer(ast)} ASC $omittedNullsFirst"
    case OrderByCriteria(ast, DescNullsFirst) =>
      stmt"${scopedTokenizer(ast)} DESC $omittedNullsFirst"
    case OrderByCriteria(ast, AscNullsLast) =>
      stmt"${scopedTokenizer(ast)} ASC $omittedNullsLast"
    case OrderByCriteria(ast, DescNullsLast) =>
      stmt"${scopedTokenizer(ast)} DESC $omittedNullsLast"
  }

  override implicit def valueTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[Value] = Tokenizer[Value] {
    case Constant(v: Boolean) if v  => stmt"1"
    case Constant(v: Boolean) if !v => stmt"0"
    case value                      => super.valueTokenizer.token(value)
  }
}

object SqliteDialect extends SqliteDialect