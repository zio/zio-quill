package io.getquill.source

import io.getquill._
import io.getquill.ast._
import io.getquill.Spec
import io.getquill.source.mirror.mirrorSource
import io.getquill.source.mirror.Row
import io.getquill.source.mirror.Row

class EncodeBindVariablesSpec extends Spec {

  "encodes bind variables" - {
    "one" in {
      val q = quote {
        (i: Int) => qr1.filter(t => t.i == i)
      }
      mirrorSource.run(q).using(1).binds mustEqual Row(1)
    }
    "two" in {
      val q = quote {
        (i: Int, j: Long) => qr1.filter(t => t.i == i && t.i > j)
      }
      mirrorSource.run(q).using(1, 2).binds mustEqual Row(1, 2L)
    }
  }

  "fails if there isn't an encoder for the binded value" in {
    val q = quote {
      (i: Thread) => qr1.filter(_.i == i)
    }
    "mirrorSource.run(q)(new Thread)" mustNot compile
  }

  "uses a custom implicit encoder" in {
    implicit val doubleEncoder = new Encoder[Row, Double] {
      override def apply(index: Int, value: Double, row: Row) =
        row.add(value)
    }
    val q = quote {
      (d: Double) => qr1.filter(_.i == d)
    }
    mirrorSource.run(q).using(1D).binds mustEqual Row(1D)
  }
}
