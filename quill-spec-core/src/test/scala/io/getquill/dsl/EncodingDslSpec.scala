package io.getquill.dsl

import io.getquill.Spec
import io.getquill.context.mirror.Row
import io.getquill.quotation.{ CaseClassValueLifting, ScalarValueLifting }
import io.getquill.testContext._

import scala.language.reflectiveCalls

case class CustomValue(i: Int) extends AnyVal

case class CustomGenericValue[T](v: T) extends AnyVal

class CustomPrivateConstructorValue private (val i: Int) extends AnyVal

object CustomPrivateConstructorValue {
  def apply(i: Int) = new CustomPrivateConstructorValue(i)
}

// Tests self lifting of `AnyVal`
case class Address(id: AddressId)

case class AddressId(value: Long) extends AnyVal {

  def `inside quotation` = quote {
    query[Address].filter(_.id == lift(this))
  }

  def `inside run - query` = run {
    query[Address].filter(_.id == lift(this))
  }

  def `inside run - insert` = run {
    query[Address].insert(_.id -> lift(this))
  }

  def `inside run - update` = run {
    query[Address].update(_.id -> lift(this))
  }
}

class EncodingDslSpec extends Spec {

  "provides factory methods for encoding" - {
    val idx = 1
    val row = Row(2)
    "Decoder" in {
      val decoder =
        (i: Index, r: ResultRow) => {
          i mustEqual idx
          r mustEqual row
          true
        }
      decoder(idx, row) mustEqual true
    }

    "Encoder" in {
      val value = 3
      val encoder =
        (i: Index, v: Int, r: ResultRow) => {
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
      val t = TestEntity("1", 2, 3L, Some(4), true)
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

  "materializes encoding for generic AnyVal" - {
    "encoder" in {
      val enc = implicitly[Encoder[CustomGenericValue[Int]]]
      enc(0, CustomGenericValue(1), Row()) mustEqual Row(1)
    }
    "decoder" in {
      val dec = implicitly[Decoder[CustomGenericValue[Int]]]
      dec(0, Row(1)) mustEqual CustomGenericValue(1)
    }
  }

  "materializes encoding for AnyVal with custom MappedEncoding" - {
    "encoder" in {
      implicit val encodeCustomValue = MappedEncoding[CustomPrivateConstructorValue, Int](_.i + 1)
      val enc = implicitly[Encoder[CustomPrivateConstructorValue]]
      enc(0, CustomPrivateConstructorValue.apply(1), Row()) mustEqual Row(2)
    }
    "decoder" in {
      implicit val decodeCustomValue = MappedEncoding[Int, CustomPrivateConstructorValue](i => CustomPrivateConstructorValue(i + 1))
      val dec = implicitly[Decoder[CustomPrivateConstructorValue]]
      dec(0, Row(1)) mustEqual CustomPrivateConstructorValue.apply(2)
    }
  }

  "materializes encoding for tagged type" - {
    object tag {
      def apply[U] = new Tagger[U]

      trait Tagged[U]
      type @@[+T, U] = T with Tagged[U]

      class Tagger[U] {
        def apply[T](t: T): T @@ U = t.asInstanceOf[T @@ U]
      }
    }

    import tag._
    sealed trait CustomTag
    type CustomInt = Int @@ CustomTag

    "encoder" in {
      implicit val encodeCustomValue = MappedEncoding[CustomInt, Int](_ + 1)
      val enc = implicitly[Encoder[CustomInt]]
      enc(0, tag[CustomTag][Int](1), Row()) mustEqual Row(2)
    }
    "decoder" in {
      implicit val decodeCustomValue = MappedEncoding[Int, CustomInt](i => tag[CustomTag][Int](i + 1))
      val dec = implicitly[Decoder[CustomInt]]
      dec(0, Row(1)) mustEqual tag[CustomTag][Int](2)
    }
  }
}