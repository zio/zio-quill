package io.getquill.sources

import io.getquill._
import io.getquill.TestSource.mirrorSource
import io.getquill.sources.mirror.Row

class SourceSpec extends Spec {

  "provides mapped encoding" - {

    case class StringValue(s: String)
    case class Entity(s: StringValue)

    "encoding" in {
      implicit val testToString = mappedEncoding[StringValue, String](_.s)
      val q = quote {
        (s: StringValue) => query[Entity].insert(_.s -> s)
      }
      mirrorSource.run(q)(List(StringValue("s"))).bindList mustEqual List(Row("s"))
    }

    "decoding" in {
      implicit val stringToTest = mappedEncoding[String, StringValue](StringValue)
      val q = quote {
        query[Entity]
      }
      mirrorSource.run(q).extractor(Row("s")) mustEqual Entity(StringValue("s"))
    }
  }
}
