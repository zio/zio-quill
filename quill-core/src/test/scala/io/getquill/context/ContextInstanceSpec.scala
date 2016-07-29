package io.getquill.context

import io.getquill.Spec
import io.getquill.context.mirror.Row
import io.getquill.testContext
import io.getquill.testContext._
import io.getquill.WrappedType
import io.getquill.WrappedValue

case class WrappedEncodable(value: Int)
  extends AnyVal with WrappedValue[Int]

class ContextInstanceSpec extends Spec {

  "provides mapped encoding" - {

    case class StringValue(s: String)
    case class Entity(s: StringValue)

    "encoding" in {
      implicit val testToString = mappedEncoding[StringValue, String](_.s)
      val q = quote {
        query[Entity].insert(_.s -> lift(StringValue("s")))
      }
      testContext.run(q).prepareRow mustEqual Row("s")
    }

    "decoding" in {
      implicit val stringToTest = mappedEncoding[String, StringValue](StringValue)
      val q = quote {
        query[Entity]
      }
      testContext.run(q).extractor(Row("s")) mustEqual Entity(StringValue("s"))
    }
  }

  "encoding set" in {
    case class Entity(i: Int)
    val q = quote {
      query[Entity].filter(e => liftQuery(Set(1)).contains(e.i))
    }
    testContext.run(q).prepareRow mustEqual Row(1)
  }

  "encodes `WrappedValue` extended value class" - {
    case class Entity(x: WrappedEncodable, s: String)

    "encoding" in {
      val q = quote {
        query[Entity].insert(_.x -> lift(WrappedEncodable(1)), _.s -> s"string")
      }
      testContext.run(q).prepareRow mustEqual Row(1)
    }

    "decoding" in {
      val q = quote {
        query[Entity]
      }
      val wrapped = WrappedEncodable(1)
      testContext.run(q).extractor(Row(1, "1")) mustEqual Entity(wrapped, wrapped.toString)
    }
  }

  "encodes constructable `WrappedType` extended class" - {
    case class Wrapped(value: Int) extends WrappedType {
      override type Type = Int
    }
    case class Entity(x: Wrapped)

    "encoding" in {
      val q = quote {
        query[Entity].insert(_.x -> lift(Wrapped(1)))
      }
      testContext.run(q).prepareRow mustEqual Row(1)
    }

    "decoding" in {
      val q = quote {
        query[Entity]
      }
      testContext.run(q).extractor(Row(1)) mustEqual Entity(Wrapped(1))
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
    "testContext.run(q).extractor(Row(1))" mustNot compile
  }
}
