package io.getquill.source.sql

import io.getquill._
import io.getquill.source.sql.test.mirrorSource
import io.getquill.source.test.Row

class SqlQueryMacroSpec extends Spec {

  "runs queries" - {
    "without bindings" - {
      "with filter" in {
        val q = quote {
          qr1.filter(t => t.s != null)
        }
        val mirror = mirrorSource.run(q)
        mirror.binds mustEqual Row()
        mirror.sql mustEqual
          "SELECT t.s, t.i, t.l FROM TestEntity t WHERE t.s IS NOT NULL"
      }
      "with map" in {
        val q = quote {
          qr1.map(t => t.s)
        }
        val mirror = mirrorSource.run(q)
        mirror.binds mustEqual Row()
        mirror.sql mustEqual
          "SELECT t.s FROM TestEntity t"
      }
      "with flatMap" in {
        val q = quote {
          qr1.flatMap(t => qr2)
        }
        val mirror = mirrorSource.run(q)
        mirror.binds mustEqual Row()
        mirror.sql mustEqual
          "SELECT x.s, x.i, x.l FROM TestEntity t, TestEntity2 x"
      }
    }
    "with bindigns" - {
      "one" in {
        val q = quote {
          (s: String) => qr1.filter(t => t.s != s)
        }
        val mirror = mirrorSource.run(q)("s")
        mirror.binds mustEqual Row("s")
        mirror.sql mustEqual
          "SELECT t.s, t.i, t.l FROM TestEntity t WHERE t.s <> ?"
      }
      "two" in {
        val q = quote {
          (s: String, i: Int) => qr1.filter(t => t.s != s && t.i != i)
        }
        val mirror = mirrorSource.run(q)("s", 1)
        mirror.binds mustEqual Row("s", 1)
        mirror.sql mustEqual
          "SELECT t.s, t.i, t.l FROM TestEntity t WHERE (t.s <> ?) AND (t.i <> ?)"
      }
    }
  }
}
