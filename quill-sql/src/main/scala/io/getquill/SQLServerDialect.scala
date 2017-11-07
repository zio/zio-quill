package io.getquill

import io.getquill.idiom.{ Token, StringToken }
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.context.sql.idiom.QuestionMarkBindVariables
import io.getquill.context.sql.idiom.ConcatSupport

trait SQLServerDialect
  extends SqlIdiom
  with QuestionMarkBindVariables
  with ConcatSupport {

  override def emptySetContainsToken(field: Token) = StringToken("1 <> 1")

  override def prepareForProbing(string: String) = string
}

object SQLServerDialect extends SQLServerDialect
