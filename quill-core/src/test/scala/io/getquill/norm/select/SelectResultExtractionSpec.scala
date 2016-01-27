package io.getquill.norm.select

import io.getquill._
import io.getquill.sources.mirror.Row
import io.getquill.TestSource.mirrorSource

case class Test(s: String, i: Int)
class SelectResultExtractionSpec extends Spec {

  "extracts the final value from a select result" - {
    "simple value" in {
      val q = quote {
        qr1.map(t => t.s)
      }
      mirrorSource.run(q)
        .extractor(Row("a")) mustEqual "a"
    }
    "case class" - {
      "simple" in {
        mirrorSource.run(qr1)
          .extractor(Row("a", 1, 2L, None)) mustEqual TestEntity("a", 1, 2L, None)
      }
      "nested" in {
        case class Inner(l: Long)
        case class Outer(s: String, i: Inner)
        val q = quote {
          query[Outer]
        }
        mirrorSource.run(q)
          .extractor(Row("a", 1L)) mustEqual Outer("a", Inner(1L))
      }
    }
    "tuple" - {
      "simple" in {
        val q = quote {
          qr1.map(t => (t.s, t.i))
        }
        mirrorSource.run(q)
          .extractor(Row("a", 1)) mustEqual (("a", 1))
      }
      "nested" in {
        val q = quote {
          qr1.map(t => (t.s, (t.i, t.l)))
        }
        mirrorSource.run(q)
          .extractor(Row("a", 1, 2L)) mustEqual (("a", (1, 2L)))
      }
    }
    "nested option" in {
      val q = quote {
        qr2.rightJoin(qr2).on((a, b) => a.s == b.s).rightJoin(qr3).on((a, b) => true).map(_._1.map(_._1.map(_.s)))
      }
      mirrorSource.run(q)
        .extractor(Row(Option("a"))) mustEqual Option(Option("a"))
    }
    "optional tuple" in {
      val q = quote {
        qr2.rightJoin(qr2).on((a, b) => a.s == b.s).map(_._1.map(t => (t.s, t.i)))
      }
      mirrorSource.run(q)
        .extractor(Row(Option("a"), Option(1))) mustEqual Some(("a", 1))
    }
    "mixed" - {
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
      "tuple with optional case class" - {
        val q = quote {
          query[Test].leftJoin(query[Test]).on((a, b) => a.s == b.s)
        }
        "defined" in {
          mirrorSource.run(q)
            .extractor(Row("a", 1, Option("b"), Option(2))) mustEqual ((Test("a", 1), Some(Test("b", 2))))
        }
        "partially defined" in {
          mirrorSource.run(q)
            .extractor(Row("a", 1, Option("b"), None)) mustEqual ((Test("a", 1), None))
        }
        "undefined" in {
          mirrorSource.run(q)
            .extractor(Row("a", 1, None, None)) mustEqual ((Test("a", 1), None))
        }
      }
    }
  }
}
