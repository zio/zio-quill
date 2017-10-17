package io.getquill.context.sql.idiom

import io.getquill._
import io.getquill.idiom.StringToken

class SQLServerDialectSpec extends Spec {

  "emptySetContainsToken" in {
    SQLServerDialect.emptySetContainsToken(StringToken("w/e")) mustBe StringToken("1 <> 1")
  }
}
