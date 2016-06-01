package io.getquill.sources.sql

import mirrorSource._

class SqlMirrorSourceSpec extends SqlSpec {
  "run non-batched action" in {
    val insert = quote { (i: Int) =>
      qr1.insert(_.i -> i)
    }
    mirrorSource.run(insert)(1) mustBe an[mirrorSource.ActionMirror]
  }
}
