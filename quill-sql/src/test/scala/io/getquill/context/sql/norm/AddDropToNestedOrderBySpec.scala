package io.getquill.context.sql.norm

import io.getquill._
import io.getquill.context.sql.{ TestDecoders, TestEncoders }

class AddDropToNestedOrderBySpec extends Spec {

  val ctx = new SqlMirrorContext(SQLServerDialect, Literal) with TestEntities with TestEncoders with TestDecoders

  import ctx._
  val q2Sorted = quote { qr2.sortBy(r => r.i) }

  "adds drop(0) to nested order-by" - {
    "simple" in {
      val q = quote {
        qr1.sortBy(r => r.i).nested
      }
      val n = quote {
        qr1.sortBy(r => r.i).drop(0).nested
      }
      translate(q) mustEqual translate(n)
    }

    "in join" in {
      val q = quote {
        qr1.join(q2Sorted).on((a, b) => a.i == b.i)
      }
      val n = quote {
        qr1.join(q2Sorted.drop(0)).on((a, b) => a.i == b.i)
      }
      translate(q) mustEqual translate(n)
    }

    "in join with union" in {
      val q = quote {
        qr1.join(q2Sorted union q2Sorted).on((a, b) => a.i == b.i)
      }
      val n = quote {
        qr1.join(q2Sorted.drop(0) union q2Sorted.drop(0)).on((a, b) => a.i == b.i)
      }
      translate(q) mustEqual translate(n)
    }

    "in join with external union" in {
      val q = quote {
        qr1.join(q2Sorted).on((a, b) => a.i == b.i) union qr1.join(q2Sorted).on((a, b) => a.i == b.i)
      }
      val n = quote {
        qr1.join(q2Sorted.drop(0)).on((a, b) => a.i == b.i) union qr1.join(q2Sorted.drop(0)).on((a, b) => a.i == b.i)
      }
      translate(q) mustEqual translate(n)
    }

    "in flat" in {
      val q = quote {
        for {
          q1 <- qr1
          q2 <- qr2.sortBy(r => r.i).join(qj => qj.i == q1.i)
        } yield (q1, q2)
      }
      val n = quote {
        for {
          q1 <- qr1
          q2 <- qr2.sortBy(r => r.i).drop(0).join(qj => qj.i == q1.i)
        } yield (q1, q2)
      }
      translate(q) mustEqual translate(n)
    }

    "in unary op" in {
      val q = quote {
        q2Sorted.nonEmpty
      }
      val n = quote {
        q2Sorted.drop(0).nonEmpty
      }
      translate(q) mustEqual translate(n)
    }

    "in unary op external" in {
      val q = quote {
        qr1.join(q2Sorted).on((a, b) => a.i == b.i).nonEmpty
      }
      val n = quote {
        qr1.join(q2Sorted.drop(0)).on((a, b) => a.i == b.i).nonEmpty
      }
      translate(q) mustEqual translate(n)
    }
  }
}
