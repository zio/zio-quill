package io.getquill

import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.context.sql.idiom.QuestionMarkBindVariables

trait MirrorSqlDialect
  extends SqlIdiom
  with QuestionMarkBindVariables {
}

object MirrorSqlDialect extends MirrorSqlDialect {
  override def prepareForProbing(string: String) = string
}
