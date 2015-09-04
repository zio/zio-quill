package io.getquill.source.sql.idiom

import io.getquill.Spec

class H2DialectSpec extends Spec {

  "mixes the nulls ordering clause" in {
    H2Dialect.isInstanceOf[NullsOrderingClause] mustEqual true
  }
}
