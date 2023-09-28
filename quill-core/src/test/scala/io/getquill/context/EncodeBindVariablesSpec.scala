package io.getquill.context

import io.getquill.base.Spec
import io.getquill.context.mirror.Row
import io.getquill.MirrorContexts.testContext
import io.getquill.MirrorContexts.testContext._

class EncodeBindVariablesSpec extends Spec {

  "encodes bind variables" - {
    "one" in {
      val i = 1
      val q = quote {
        qr1.filter(t => t.i == lift(i))
      }
      testContext.run(q).prepareRow mustEqual Row(i)
    }
    "three" in {
      val i = 1
      val j = 2
      val o = 3
      val q = quote {
        qr1.filter(t => t.i == lift(i) && t.i > lift(j) && t.o.forall(_ == lift(o)))
      }
      testContext.run(q).prepareRow mustEqual Row(i, j, o)
    }
  }

  "fails if there isn't an encoder for the bound value" in {
    quote { (i: Thread) =>
      qr1.map(_ => i)
    }
    "testContext.run(q)(new Thread)" mustNot compile
  }

  "uses a custom implicit encoder" in {
    implicit val doubleEncoder = testContext.encoder[Double]
    val d                      = 1d
    val q = quote {
      qr1.map(_ => lift(d))
    }
    testContext.run(q).prepareRow mustEqual Row(1d)
  }

  "fails for not value class without encoder" in {
    final case class NotValueClass(value: Int)
    final case class Entity(x: NotValueClass)
    quote { (x: NotValueClass) =>
      query[Entity].filter(_.x == x)
    }
    "testContext.run(q)(NotValueClass(1))" mustNot compile
  }
}
