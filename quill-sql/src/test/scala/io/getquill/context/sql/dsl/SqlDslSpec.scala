package io.getquill.context.sql.dsl

import io.getquill.Spec
import io.getquill.context.sql.testContext._
import io.getquill.context.sql.testContext

class SqlDslSpec extends Spec {

  "like" - {
    "constant" in {
      val q = quote {
        query[TestEntity].filter(t => Like(t.s) like "a")
      }
      testContext.run(q).string mustEqual "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t WHERE t.s like 'a'"
    }
    "string interpolation" in {
      val q = quote {
        query[TestEntity].filter(t => t.s like s"%${lift("a")}%")
      }
      testContext.run(q).string mustEqual "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t WHERE t.s like ('%' || ?) || '%'"
    }
  }
}
