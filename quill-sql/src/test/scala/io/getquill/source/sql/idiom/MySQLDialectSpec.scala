package io.getquill.source.sql.idiom

import io.getquill.Spec

class MySQLDialectSpec extends Spec {

  "mixes the workaround for offset without limit" in {
    MySQLDialect.isInstanceOf[OffsetWithoutLimitWorkaround] mustEqual true
  }
}
