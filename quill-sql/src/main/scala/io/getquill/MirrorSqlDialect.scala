package io.getquill

import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.context.sql.idiom.QuestionMarkBindVariables
import io.getquill.context.sql.idiom.ConcatSupport

trait MirrorSqlDialect
  extends SqlIdiom
  with QuestionMarkBindVariables
  with ConcatSupport

object MirrorSqlDialect extends MirrorSqlDialect {
  override def prepareForProbing(string: String) = string
}
