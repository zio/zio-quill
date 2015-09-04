package io.getquill.source.sql.idiom

import io.getquill.Spec

class PostgresDialectSpec extends Spec {

  "mixes the nulls ordering clause" in {
    PostgresDialect.isInstanceOf[NullsOrderingClause] mustEqual true
  }
}
