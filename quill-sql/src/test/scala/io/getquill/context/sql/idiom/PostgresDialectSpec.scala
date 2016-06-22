package io.getquill.context.sql.idiom

import io.getquill.context.sql.SqlSpec

class PostgresDialectSpec extends SqlSpec {

  "supports the `prepare` statement" in {
    val sql = "test"
    PostgresDialect.prepare(sql) mustEqual
      s"PREPARE p${PostgresDialect.preparedStatementId} AS $sql"
  }
}
