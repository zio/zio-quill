package io.getquill.source

import io.getquill._
import io.getquill.ast._
import io.getquill.Spec
import io.getquill.test.mirrorSource
import io.getquill.test.Row

class EncodeBindVariablesSpec extends Spec {

  "encodes bind variables" - {
    "one" in {
      val q = quote {
        (i: Int) => qr1.filter(t => t.i == i)
      }
      mirrorSource.run(q)(1).binds mustEqual Row(1)
    }
    "two" in {
      val q = quote {
        (i: Int, j: Int) => qr1.filter(t => t.i == i && t.i > j)
      }
      mirrorSource.run(q)(1, 2).binds mustEqual Row(1, 2)
    }
  }

  "fails if there isn't an encoder for the binded value" in {
    val q = quote {
      (i: Thread) => qr1.filter(_.i == i)
    }
    "mirrorSource.run(q)(new Thread)" mustNot compile
  }
}
