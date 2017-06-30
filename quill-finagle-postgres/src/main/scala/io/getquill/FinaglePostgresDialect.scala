package io.getquill

import io.getquill.context.sql.idiom.PositionalBindVariables

trait FinaglePostgresDialect
  extends PostgresDialect
  with PositionalBindVariables

object FinaglePostgresDialect extends FinaglePostgresDialect
