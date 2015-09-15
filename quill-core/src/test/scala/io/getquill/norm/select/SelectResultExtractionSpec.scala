package io.getquill.norm.select

import io.getquill._
import io.getquill.source.mirror.Row
import io.getquill.source.mirror.mirrorSource

class SelectResultExtractionSpec extends Spec {

  "extracts the final value from a select result" - {
    "simple value" in {
      val q = quote {
        qr1.map(t => t.s)
      }
      mirrorSource.run(q)
        .extractor(Row("a")) mustEqual "a"
    }
    "case class" in {
      mirrorSource.run(qr1)
        .extractor(Row("a", 1, 2L)) mustEqual TestEntity("a", 1, 2L)
    }
    "simple values and case classes" - {
      "case class in the beginning" in {
        val q = quote {
          qr1.map(t => (t, t.s))
        }
        mirrorSource.run(q)
          .extractor(Row("a", 1, 2L, "b")) mustEqual ((TestEntity("a", 1, 2L), "b"))
      }
      "case class in the end" in {
        val q = quote {
          qr1.map(t => (t.s, t))
        }
        mirrorSource.run(q)
          .extractor(Row("b", "a", 1, 2L)) mustEqual (("b", TestEntity("a", 1, 2L)))
      }
      "case class in the middle" in {
        val q = quote {
          qr1.map(t => (t.s, t, t.i))
        }
        mirrorSource.run(q)
          .extractor(Row("b", "a", 1, 2L, 3)) mustEqual (("b", TestEntity("a", 1, 2L), 3))
      }
      "two case classes" in {
        val q = quote {
          qr1.flatMap(x => qr2.map(y => (x, y)))
        }
        mirrorSource.run(q)
          .extractor(Row("a", 1, 2L, "b", 3, 4L)) mustEqual ((TestEntity("a", 1, 2L), TestEntity2("b", 3, 4L)))
      }
    }
  }
}
