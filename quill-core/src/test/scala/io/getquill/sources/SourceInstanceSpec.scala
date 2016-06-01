package io.getquill.sources

import io.getquill.testSource._
import io.getquill.testSource
import io.getquill._

import io.getquill.sources.mirror.Row

case class WrappedEncodable(value: Int)
  extends AnyVal with WrappedValue[Int]

class SourceInstanceSpec extends Spec {

  "provides mapped encoding" - {

    case class StringValue(s: String)
    case class Entity(s: StringValue)

    "encoding" in {
      implicit val testToString = mappedEncoding[StringValue, String](_.s)
      val q = quote {
        (s: StringValue) => query[Entity].insert(_.s -> s)
      }
      testSource.run(q)(List(StringValue("s"))).bindList mustEqual List(Row("s"))
    }

    "decoding" in {
      implicit val stringToTest = mappedEncoding[String, StringValue](StringValue)
      val q = quote {
        query[Entity]
      }
      testSource.run(q).extractor(Row("s")) mustEqual Entity(StringValue("s"))
    }
  }

  "encoding set" in {
    case class Entity(i: Int)
    val q = quote { (is: Set[Int]) =>
      query[Entity].filter(e => is.contains(e.i))
    }
    testSource.run(q)(Set(1)).binds mustEqual Row(Set(1))
  }

  "encodes `WrappedValue` extended value class" - {
    case class Entity(x: WrappedEncodable, s: String)

    "encoding" in {
      val q = quote {
        (x: WrappedEncodable) => query[Entity].insert(_.x -> x, _.s -> s"$x")
      }
      testSource.run(q)(List(WrappedEncodable(1))).bindList mustEqual List(Row(1))
    }

    "decoding" in {
      val q = quote {
        query[Entity]
      }
      val wrapped = WrappedEncodable(1)
      testSource.run(q).extractor(Row(1, "1")) mustEqual Entity(wrapped, wrapped.toString)
    }
  }

  "encodes constructable `WrappedType` extended class" - {
    case class Wrapped(value: Int) extends WrappedType {
      override type Type = Int
    }
    case class Entity(x: Wrapped)

    "encoding" in {
      val q = quote {
        (x: Wrapped) => query[Entity].insert(_.x -> x)
      }
      testSource.run(q)(List((Wrapped(1)))).bindList mustEqual List(Row(1))
    }

    "decoding" in {
      val q = quote {
        query[Entity]
      }
      testSource.run(q).extractor(Row(1)) mustEqual Entity(Wrapped(1))
    }
  }

  "fails to encode non-constructable `WrappedType` extended class" in {
    trait Wrapper[T] extends WrappedType {
      override type Type = T
    }
    case class Entity(x: Wrapper[Int])

    val q = quote {
      query[Entity]
    }
    "testSource.run(q).extractor(Row(1))" mustNot compile
  }
}
