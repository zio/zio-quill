package io.getquill

import io.getquill.ast.{ Ast, OnConflict }
import io.getquill.context.sql.idiom._
import io.getquill.idiom.StatementInterpolator.Tokenizer
import io.getquill.idiom.{ StringToken, Token }

trait SqliteDialect
  extends SqlIdiom
  with QuestionMarkBindVariables
  with NoConcatSupport
  with OnConflictSupport {

  override def emptySetContainsToken(field: Token) = StringToken("0")

  override def prepareForProbing(string: String) = s"sqlite3_prepare_v2($string)"

  override def astTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[Ast] =
    Tokenizer[Ast] {
      case c: OnConflict => conflictTokenizer.token(c)
      case ast           => super.astTokenizer.token(ast)
    }
}

object SqliteDialect extends SqliteDialect
