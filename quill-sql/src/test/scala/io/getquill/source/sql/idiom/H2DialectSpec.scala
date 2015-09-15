package io.getquill.source.sql.idiom

import io.getquill.Spec

class H2DialectSpec extends Spec {

  "mixes the nulls ordering clause" in {
    H2Dialect.isInstanceOf[NullsOrderingClause] mustEqual true
  }

  "mixes the workaround for offset without limit" in {
    H2Dialect.isInstanceOf[OffsetWithoutLimitWorkaround] mustEqual true
  }
}
