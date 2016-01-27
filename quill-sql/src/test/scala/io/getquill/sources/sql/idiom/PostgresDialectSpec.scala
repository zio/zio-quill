package io.getquill.sources.sql.idiom

import io.getquill.Spec

class PostgresDialectSpec extends Spec {

  "supports the `prepare` statement" in {
    val sql = "test"
    PostgresDialect.prepare(sql) mustEqual
      s"PREPARE p${PostgresDialect.preparedStatementId} AS $sql"
  }
}
