package io.getquill.sources.sql.idiom

import java.util.concurrent.atomic.AtomicInteger

trait H2Dialect
  extends SqlIdiom
  with PositionalVariables {

  private[idiom] val preparedStatementId = new AtomicInteger

  override def prepare(sql: String) =
    s"PREPARE p${preparedStatementId.incrementAndGet} AS ${positionalVariables(sql)}"
}

object H2Dialect extends H2Dialect
