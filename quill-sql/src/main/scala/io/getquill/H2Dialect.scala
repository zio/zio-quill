package io.getquill

import java.util.concurrent.atomic.AtomicInteger
import io.getquill.context.sql.idiom.PositionalVariables
import io.getquill.context.sql.idiom.SqlIdiom

trait H2Dialect
  extends SqlIdiom
  with PositionalVariables {

  private[getquill] val preparedStatementId = new AtomicInteger

  override def prepare(sql: String) =
    s"PREPARE p${preparedStatementId.incrementAndGet} AS ${positionalVariables(sql)}"
}

object H2Dialect extends H2Dialect
