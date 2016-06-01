package io.getquill.context.sql.idiom

import io.getquill.Spec
import io.getquill.PostgresDialect

class PostgresDialectSpec extends Spec {

  "supports the `prepare` statement" in {
    val sql = "test"
    PostgresDialect.prepare(sql) mustEqual
      s"PREPARE p${PostgresDialect.preparedStatementId} AS $sql"
  }
}
