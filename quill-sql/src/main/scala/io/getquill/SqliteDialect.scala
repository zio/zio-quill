package io.getquill

import io.getquill.idiom.{ Token, StringToken }
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.context.sql.idiom.QuestionMarkBindVariables

trait SqliteDialect
  extends SqlIdiom
  with QuestionMarkBindVariables {

  override def emptySetContainsToken(field: Token) = StringToken("0")

  override def prepareForProbing(string: String) = s"sqlite3_prepare_v2($string)"
}

object SqliteDialect extends SqliteDialect
