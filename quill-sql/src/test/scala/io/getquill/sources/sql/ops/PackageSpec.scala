package io.getquill.sources.sql.ops

import io.getquill._
import io.getquill.sources.sql.mirrorSource

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
      mirrorSource.run(q)("a").sql mustEqual "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.s like ('%' || ?) || '%'"
    }
  }
}
