package io.getquill.context.sql

import mirrorContext._
import io.getquill.context.mirror.Row

class SqlQueryMacroSpec extends SqlSpec {

  "runs queries" - {
    "without bindings" - {
      "with filter" in {
        val q = quote {
          qr1.filter(t => t.s != null)
        }
        val mirror = mirrorContext.run(q)
        mirror.binds mustEqual Row()
        mirror.extractor(Row("s", 1, 2L, None)) mustEqual TestEntity("s", 1, 2L, None)
        mirror.sql mustEqual "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.s IS NOT NULL"
      }
      "with map" in {
        val q = quote {
          qr1.map(t => (t.s, t.i, t.l))
        }
        val mirror = mirrorContext.run(q)
        mirror.binds mustEqual Row()
        mirror.extractor(Row("s", 1, 2L)) mustEqual (("s", 1, 2L))
        mirror.sql mustEqual "SELECT t.s, t.i, t.l FROM TestEntity t"
      }
      "with flatMap" in {
        val q = quote {
          qr1.flatMap(t => qr2)
        }
        val mirror = mirrorContext.run(q)
        mirror.binds mustEqual Row()
        mirror.extractor(Row("s", 1, 2L, None)) mustEqual TestEntity2("s", 1, 2L, None)
        mirror.sql mustEqual "SELECT x.s, x.i, x.l, x.o FROM TestEntity t, TestEntity2 x"
      }
    }
    "with bindigns" - {
      "one" in {
        val q = quote {
          (s: String) => qr1.filter(t => t.s != s)
        }
        val mirror = mirrorContext.run(q)("s")
        mirror.binds mustEqual Row("s")
        mirror.sql mustEqual
          "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.s <> ?"
      }
      "two" in {
        val q = quote {
          (l: Long, i: Int) => qr1.filter(t => t.l != l && t.i != i)
        }
        val mirror = mirrorContext.run(q)(2L, 1)
        mirror.binds mustEqual Row(2L, 1)
        mirror.sql mustEqual
          "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE (t.l <> ?) AND (t.i <> ?)"
      }
    }
  }
}
