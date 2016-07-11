package io.getquill.sources.sql

import io.getquill.sources.BindedStatementBuilder

class SqlBindedStatementBuilder[S] extends BindedStatementBuilder[S] {
  override def emptySet: String = "SELECT NULL LIMIT 0"
}
