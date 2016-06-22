package io.getquill.context.sql.idiom

import java.util.concurrent.atomic.AtomicInteger

trait PostgresDialect
  extends SqlIdiom
  with PositionalVariables {

  private[idiom] val preparedStatementId = new AtomicInteger

  override def prepare(sql: String) =
    s"PREPARE p${preparedStatementId.incrementAndGet} AS ${positionalVariables(sql)}"
}

object PostgresDialect extends PostgresDialect
