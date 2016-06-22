package io.getquill.context.sql.ops

import io.getquill.context.sql.mirrorContext._
import io.getquill.context.sql.mirrorContext
import io.getquill.context.sql.mirrorContext
import io.getquill.context.sql.SqlSpec

class PackageSpec extends SqlSpec {

  "like" - {
    "constant" in {
      val q = quote {
        query[TestEntity].filter(t => t.s like "a")
      }
      mirrorContext.run(q).sql mustEqual "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.s like 'a'"
    }
    "string interpolation" in {
      val q = quote {
        (a: String) =>
          query[TestEntity].filter(t => t.s like s"%$a%")
      }
      mirrorContext.run(q)("a").sql mustEqual "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.s like ('%' || ?) || '%'"
    }
  }
}
