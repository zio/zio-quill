package io.getquill.context

import io.getquill.Spec
import io.getquill.context.mirror.Row
import io.getquill.testContext
import io.getquill.testContext._
import io.getquill.WrappedType

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
      (i: Thread) => qr1.filter(_.i == i)
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
      qr1.filter(_.i == lift(d))
    }
    testContext.run(q).prepareRow mustEqual Row(1D)
  }

  "encodes bind variables for wrapped types" - {

    "encodes `WrappedValue` extended value class" in {
      case class Entity(x: WrappedEncodable)
      val q = quote {
        query[Entity].filter(_.x == lift(WrappedEncodable(1)))
      }
      val r = testContext.run(q)
      r.string mustEqual "query[Entity].filter(x3 => x3.x == ?).map(x3 => x3.x)"
      r.prepareRow mustEqual Row(1)
    }

    "encodes constructable `WrappedType` extended class" in {
      case class Wrapped(value: Int) extends WrappedType {
        override type Type = Int
      }
      case class Entity(x: Wrapped)

      val q = quote {
        query[Entity].filter(_.x == lift(Wrapped(1)))
      }
      val r = testContext.run(q)
      r.string mustEqual "query[Entity].filter(x4 => x4.x == ?).map(x4 => x4.x)"
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
