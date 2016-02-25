package io.getquill.sources.sql

import io.getquill._

class SqlMirrorSourceSpec extends Spec {
  "run non-batched action" in {
    val insert = quote { (i: Int) =>
      qr1.insert(_.i -> i)
    }
    mirrorSource.run(insert)(1) mustBe an[mirrorSource.ActionMirror]
  }
}
