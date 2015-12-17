package io.getquill.source.sql.ops

import io.getquill._
import io.getquill.source.sql.mirror.mirrorSource

class PackageSpec extends Spec {

  "like" - {
    "constant" in {
      val q = quote {
        query[TestEntity].filter(t => t.s like "a")
      }
      mirrorSource.run(q).sql mustEqual "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.s like 'a'"
    }
    "string interpolation" in {
      val q = quote {
        (a: String) =>
          query[TestEntity].filter(t => t.s like s"%$a%")
      }
      mirrorSource.run(q).using("a").sql mustEqual "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.s like ('%' || ?) || '%'"
    }
  }

  "in" - {
    "constant int" in {
      val q = quote {
        query[TestEntity].filter(t => t.i in List(1, 2))
      }
      mirrorSource.run(q).sql mustEqual "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.i IN (1,2)"
    }
    "constant string" in {
      val q = quote {
        query[TestEntity].filter(t => t.s IN List("a", "b"))
      }
      mirrorSource.run(q).sql mustEqual "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.s IN ('a', 'b')"
    }
  }
}
