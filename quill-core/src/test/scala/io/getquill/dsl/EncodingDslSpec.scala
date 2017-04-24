package io.getquill.dsl

import io.getquill.Spec
import io.getquill.context.mirror.Row
import io.getquill.quotation.{ CaseClassValueLifting, ScalarValueLifting }
import io.getquill.testContext._

import scala.language.reflectiveCalls
import scala.reflect.ClassTag

case class CustomValue(i: Int) extends AnyVal

case class CustomGenericValue[T](v: T) extends AnyVal

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

  "provide mapped encoding for traversables collections" - {
    case class Wrap(intStr: String)

    implicit def encoderTravInt[T <: Traversable[Int]]: Encoder[T] = encoder[T]
    implicit def decoderTravInt[T <: Traversable[Int]: ClassTag]: Decoder[T] = decoder[T]

    implicit val encoderWrap = MappedEncoding[Wrap, Int](_.intStr.toInt)
    implicit val decoderWrap = MappedEncoding[Int, Wrap](x => Wrap(x.toString))

    "encoder" in {
      val enc = implicitly[Encoder[Seq[Wrap]]]
      enc(0, Seq(Wrap("123")), Row()) mustEqual Row(Seq(123))
    }

    "decoder" in {
      val dec = implicitly[Decoder[List[Wrap]]]
      dec(0, Row(List(123))) mustEqual List(Wrap("123"))
    }

    case class Wrap2(str: String)
    implicit val encoderWrap2 = MappedEncoding[Wrap2, String](_.str)
    implicit val decoderWrap2 = MappedEncoding[String, Wrap2](Wrap2.apply)

    "mapped trav encoding should not compile without base trav encoders/decoders" in {
      "implicitly[Encoder[Seq[Wrap2]]]" mustNot compile
      "implicitly[Decoder[Seq[Wrap2]]]" mustNot compile
    }
  }
}