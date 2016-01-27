package io.getquill.sources.sql.idiom

import java.util.concurrent.atomic.AtomicInteger

trait PostgresDialect
    extends SqlIdiom {

  private[idiom] val preparedStatementId = new AtomicInteger

  override def prepare(sql: String) =
    s"PREPARE p${preparedStatementId.incrementAndGet} AS ${positionalVariables(sql)}"

  private def positionalVariables(sql: String) =
    sql.foldLeft((1, "")) {
      case ((idx, s), '?') =>
        (idx + 1, s + "$" + idx)
      case ((idx, s), c) =>
        (idx, s + c)
    }._2
}

object PostgresDialect extends PostgresDialect
