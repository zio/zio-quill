package io.getquill.dsl

import io.getquill.Spec
import io.getquill.testContext._
import io.getquill.context.mirror.Row
import io.getquill.quotation.ScalarValueLifting
import io.getquill.quotation.CaseClassValueLifting
import scala.language.reflectiveCalls

case class CustomValue(i: Int) extends AnyVal

class EncodingDslSpec extends Spec {

  "provides factory methods for encoding" - {
    val idx = 1
    val row = Row(2)
    "Decoder" in {
      val decoder = Decoder[Boolean] {
        (i, r) =>
          i mustEqual idx
          r mustEqual row
          true
      }
      decoder(idx, row) mustEqual true
    }

    "Encoder" in {
      val value = 3
      val encoder = Encoder[Int] {
        (i, v, r) =>
          i mustEqual idx
          v mustEqual value
          r mustEqual row
          row
      }
      encoder(idx, value, row) mustEqual row
    }
  }

  "lifts values" - {
    "scalar" in {
      val i = 89890
      val q = quote {
        lift(i)
      }
      q.liftings.`i` match {
        case ScalarValueLifting(value, encoder) =>
          value mustEqual i
          encoder mustEqual intEncoder
      }
    }
    "case class" in {
      val t = TestEntity("1", 2, 3L, Some(4))
      val q = quote {
        lift(t)
      }
      q.liftings.`t` match {
        case CaseClassValueLifting(value) =>
          value mustEqual t
      }
    }
    "failure (no encoder, not case class)" in {
      "quote(lift(this))" mustNot compile
    }
  }

  "materializes encoding for AnyVal" - {
    "encoder" in {
      val enc = implicitly[Encoder[CustomValue]]
      enc(0, CustomValue(1), Row()) mustEqual Row(1)
    }
    "decoder" in {
      val dec = implicitly[Decoder[CustomValue]]
      dec(0, Row(1)) mustEqual CustomValue(1)
    }
  }
}