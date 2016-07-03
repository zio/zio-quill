package io.getquill.context.sql

import io.getquill.context.BindedStatementBuilder

class SqlBindedStatementBuilder[S] extends BindedStatementBuilder[S] {
  override def emptySet: String = "SELECT 0 WHERE FALSE"
}
