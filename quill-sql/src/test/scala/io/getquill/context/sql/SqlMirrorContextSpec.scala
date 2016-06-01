package io.getquill.context.sql

import io.getquill.Spec
import io.getquill.context.sql.testContext.qr1
import io.getquill.context.sql.testContext.quote
import io.getquill.context.sql.testContext.unquote

class SqlMirrorContextSpec extends Spec {
  "run non-batched action" in {
    val insert = quote { (i: Int) =>
      qr1.insert(_.i -> i)
    }
    testContext.run(insert)(1) mustBe an[testContext.ActionMirror]
  }
}
