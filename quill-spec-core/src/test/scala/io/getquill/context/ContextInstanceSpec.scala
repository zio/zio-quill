package io.getquill.context

import io.getquill.Spec
import io.getquill.context.mirror.Row
import io.getquill.testContext
import io.getquill.testContext._

case class ValueClass(value: Int) extends AnyVal

case class GenericValueClass[T](value: T) extends AnyVal

class ContextInstanceSpec extends Spec {

  "provides mapped encoding" - {

    case class StringValue(s: String)
    case class Entity(s: StringValue)

    "context-based" - {
      "encoding" in {
        implicit val testToString = MappedEncoding[StringValue, String](_.s)
        val q = quote {
          query[Entity].insert(_.s -> lift(StringValue("s")))
        }
        testContext.run(q).prepareRow mustEqual Row("s")
      }

      "decoding" in {
        implicit val stringToTest = MappedEncoding[String, StringValue](StringValue)
        val q = quote {
          query[Entity]
        }
        testContext.run(q).extractor(Row("s")) mustEqual Entity(StringValue("s"))
      }
    }
    "package-based" - {
      import io.getquill.MappedEncoding
      "encoding" in {
        implicit val testToString = MappedEncoding[StringValue, String](_.s)
        val q = quote {
          query[Entity].insert(_.s -> lift(StringValue("s")))
        }
        testContext.run(q).prepareRow mustEqual Row("s")
      }

      "decoding" in {
        implicit val stringToTest = MappedEncoding[String, StringValue](StringValue)
        val q = quote {
          query[Entity]
        }
        testContext.run(q).extractor(Row("s")) mustEqual Entity(StringValue("s"))
      }
    }
  }

  "encoding set" in {
    case class Entity(i: Int)
    val q = quote {
      query[Entity].filter(e => liftQuery(Set(1)).contains(e.i))
    }
    testContext.run(q).prepareRow mustEqual Row(1)
  }

  "encodes value class" - {
    case class Entity(x: ValueClass, s: String)

    "encoding" in {
      val q = quote {
        query[Entity].insert(_.x -> lift(ValueClass(1)), _.s -> "string")
      }
      testContext.run(q).prepareRow mustEqual Row(1)
    }

    "decoding" in {
      val q = quote {
        query[Entity]
      }
      val v = ValueClass(1)
      testContext.run(q).extractor(Row(1, "1")) mustEqual Entity(v, "1")
    }
  }

  "encodes generic value class" - {
    case class Entity(x: GenericValueClass[Int], s: String)

    "encoding" in {
      val q = quote {
        query[Entity].insert(_.x -> lift(GenericValueClass(1)), _.s -> "string")
      }
      testContext.run(q).prepareRow mustEqual Row(1)
    }

    "decoding" in {
      val q = quote {
        query[Entity]
      }
      val v = GenericValueClass(1)
      testContext.run(q).extractor(Row(1, "1")) mustEqual Entity(v, "1")
    }
  }
}
