package io.getquill.source

import io.getquill._
import io.getquill.source.mirror.mirrorSource
import io.getquill.source.mirror.Row

class SourceSpec extends Spec {

  "loads the config" in {
    mirrorSource.mirrorConfig.getString("testKey") mustEqual "testValue"
  }

  "provides mapped encoding" - {

    case class StringValue(s: String)
    case class Entity(s: StringValue)

    "encoding" in {
      implicit val testToString = mappedEncoding[StringValue, String](_.s)
      val q = quote {
        (s: StringValue) => query[Entity].insert(_.s -> s)
      }
      mirrorSource.run(q).using(List(StringValue("s"))).bindList mustEqual List(Row("s"))
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
