package io.getquill.context.sql.ops

import io.getquill.Spec
import io.getquill.context.sql.testContext.Like
import io.getquill.context.sql.testContext.TestEntity
import io.getquill.context.sql.testContext.query
import io.getquill.context.sql.testContext.quote
import io.getquill.context.sql.testContext.unquote
import io.getquill.context.sql.testContext

class PackageSpec extends Spec {

  "like" - {
    "constant" in {
      val q = quote {
        query[TestEntity].filter(t => t.s like "a")
      }
      testContext.run(q).sql mustEqual "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.s like 'a'"
    }
    "string interpolation" in {
      val q = quote {
        (a: String) =>
          query[TestEntity].filter(t => t.s like s"%$a%")
      }
      testContext.run(q)("a").sql mustEqual "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.s like ('%' || ?) || '%'"
    }
  }
}
