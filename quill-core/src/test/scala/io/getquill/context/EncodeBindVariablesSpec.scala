package io.getquill.context

import io.getquill.Spec
import io.getquill.context.mirror.Row
import io.getquill.testContext
import io.getquill.testContext._

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
      val o = Option(3)
      val q = quote {
        qr1.filter(t => t.i == lift(i) && t.i > lift(j) && t.o == lift(o))
      }
      testContext.run(q).prepareRow mustEqual Row(i, j, o)
    }
  }

  "fails if there isn't an encoder for the binded value" in {
    val q = quote {
      (i: Thread) => qr1.map(t => i)
    }
    "testContext.run(q)(new Thread)" mustNot compile
  }

  "uses a custom implicit encoder" in {
    implicit val doubleEncoder = new testContext.Encoder[Double] {
      override def apply(index: Int, value: Double, row: Row) =
        row.add(value)
    }
    val d = 1D
    val q = quote {
      qr1.map(t => lift(d))
    }
    testContext.run(q).prepareRow mustEqual Row(1D)
  }

  "encodes bind variables for wrapped types" - {

    "encodes `WrappedValue` extended value class" in {
      case class Entity(x: WrappedEncodable)
      val q = quote {
        query[Entity].filter(t => t.x == lift(WrappedEncodable(1)))
      }
      val r = testContext.run(q)
      r.string mustEqual "query[Entity].filter(t => t.x == ?).map(t => t.x)"
      r.prepareRow mustEqual Row(1)
    }

    "fails for unwrapped class" in {
      case class WrappedNotEncodable(value: Int)
      case class Entity(x: WrappedNotEncodable)
      val q = quote {
        (x: WrappedNotEncodable) => query[Entity].filter(_.x == x)
      }
      "testContext.run(q)(WrappedNotEncodable(1))" mustNot compile
    }
  }
}
