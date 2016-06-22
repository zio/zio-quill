package io.getquill.context.sql

import mirrorContext._

class SqlMirrorContextSpec extends SqlSpec {
  "run non-batched action" in {
    val insert = quote { (i: Int) =>
      qr1.insert(_.i -> i)
    }
    mirrorContext.run(insert)(1) mustBe an[mirrorContext.ActionMirror]
  }
}
