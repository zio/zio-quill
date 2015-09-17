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
        .extractor(Row("a", 1, 2L, None)) mustEqual TestEntity("a", 1, 2L, None)
    }
    "simple values and case classes" - {
      "case class in the beginning" in {
        val q = quote {
          qr1.map(t => (t, t.s))
        }
        mirrorSource.run(q)
          .extractor(Row("a", 1, 2L, Some(1), "b")) mustEqual ((TestEntity("a", 1, 2L, Some(1)), "b"))
      }
      "case class in the end" in {
        val q = quote {
          qr1.map(t => (t.s, t))
        }
        mirrorSource.run(q)
          .extractor(Row("b", "a", 1, 2L, None)) mustEqual (("b", TestEntity("a", 1, 2L, None)))
      }
      "case class in the middle" in {
        val q = quote {
          qr1.map(t => (t.s, t, t.i))
        }
        mirrorSource.run(q)
          .extractor(Row("b", "a", 1, 2L, None, 3)) mustEqual (("b", TestEntity("a", 1, 2L, None), 3))
      }
      "two case classes" in {
        val q = quote {
          qr1.flatMap(x => qr2.map(y => (x, y)))
        }
        mirrorSource.run(q)
          .extractor(Row("a", 1, 2L, None, "b", 3, 4L, Some(1))) mustEqual ((TestEntity("a", 1, 2L, None), TestEntity2("b", 3, 4L, Some(1))))
      }
    }
  }
}
