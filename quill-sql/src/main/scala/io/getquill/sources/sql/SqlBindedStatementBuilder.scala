package io.getquill.sources.sql

import io.getquill.sources.BindedStatementBuilder

class SqlBindedStatementBuilder[S] extends BindedStatementBuilder[S] {
  override def emptySet: String = "SELECT 0 FROM (SELECT 0) AS TBL WHERE FALSE"
}
