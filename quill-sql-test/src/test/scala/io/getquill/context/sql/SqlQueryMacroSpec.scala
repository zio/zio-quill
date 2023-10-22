package io.getquill.context.sql

import io.getquill.base.Spec
import io.getquill.context.mirror.{MirrorSession, Row}
import io.getquill.context.sql.testContext._

class SqlQueryMacroSpec extends Spec {

  "runs queries" - {
    "without bindings" - {
      "with filter" in {
        val q = quote {
          qr1.filter(t => t.s != null)
        }
        val mirror = testContext.run(q)
        mirror.prepareRow mustEqual Row()
        mirror
          .extractor(Row("s", 1, 2L, null, true), MirrorSession.default) mustEqual TestEntity("s", 1, 2L, None, true)
        mirror.string mustEqual "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t WHERE t.s IS NOT NULL"
      }
      "with map" in {
        val q = quote {
          qr1.map(t => (t.s, t.i, t.l))
        }
        val mirror = testContext.run(q)
        mirror.prepareRow mustEqual Row()
        mirror.extractor(Row("s", 1, 2L), MirrorSession.default) mustEqual (("s", 1, 2L))
        mirror.string mustEqual "SELECT t.s AS _1, t.i AS _2, t.l AS _3 FROM TestEntity t"
      }
      "with flatMap" in {
        val q = quote {
          qr1.flatMap(t => qr2)
        }
        val mirror = testContext.run(q)
        mirror.prepareRow mustEqual Row()
        mirror.extractor(Row("s", 1, 2L, null), MirrorSession.default) mustEqual TestEntity2("s", 1, 2L, None)
        mirror.string mustEqual "SELECT x.s, x.i, x.l, x.o FROM TestEntity t, TestEntity2 x"
      }
    }
    "with bindings" - {
      "one" in {
        val q = quote {
          qr1.filter(t => t.s != lift("s"))
        }
        val mirror = testContext.run(q)
        mirror.prepareRow mustEqual Row("s")
        mirror.string mustEqual
          "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t WHERE t.s <> ?"
      }
      "two" in {
        val q = quote {
          qr1.filter(t => t.l != lift(2L) && t.i != lift(1))
        }
        val mirror = testContext.run(q)
        mirror.prepareRow mustEqual Row(2L, 1)
        mirror.string mustEqual
          "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t WHERE t.l <> ? AND t.i <> ?"
      }
    }
  }
}
