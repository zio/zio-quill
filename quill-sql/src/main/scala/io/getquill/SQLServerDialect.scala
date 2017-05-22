package io.getquill

import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.context.sql.idiom.QuestionMarkBindVariables

trait SQLServerDialect extends SqlIdiom with QuestionMarkBindVariables {

  override def emptyQuery = "SELECT TOP 0 NULL"

  override def prepareForProbing(string: String) = string
}

object SQLServerDialect extends SQLServerDialect