package io.getquill.context.sql.dsl

import io.getquill.Spec
import io.getquill.context.sql.testContext
import io.getquill.context.sql.testContext._

class SqlDslSpec extends Spec {

  case class SqlDslTestEntity(s: String, i: Int, l: Long, o: Option[String], b: Boolean)

  "like for String" - {
    "constant" in {
      val q = quote {
        query[SqlDslTestEntity].filter(t => Like(t.s) like "a")
      }
      testContext.run(q).string mustEqual "SELECT t.s, t.i, t.l, t.o, t.b FROM SqlDslTestEntity t WHERE t.s like 'a'"
    }
    "string interpolation" in {
      val q = quote {
        query[SqlDslTestEntity].filter(t => t.s like s"%${lift("a")}%")
      }
      testContext.run(q).string mustEqual "SELECT t.s, t.i, t.l, t.o, t.b FROM SqlDslTestEntity t WHERE t.s like ('%' || ?) || '%'"
    }
  }

  "like for Option[String]" - {

    "constant" in {
      val q = quote {
        query[SqlDslTestEntity].filter(t => LikeOption(t.o) like "a")
      }
      testContext.run(q).string mustEqual "SELECT t.s, t.i, t.l, t.o, t.b FROM SqlDslTestEntity t WHERE t.o like 'a'"
    }
    "string interpolation" in {
      val q = quote {
        query[SqlDslTestEntity].filter(t => t.o like s"%${lift("a")}%")
      }
      testContext.run(q).string mustEqual "SELECT t.s, t.i, t.l, t.o, t.b FROM SqlDslTestEntity t WHERE t.o like ('%' || ?) || '%'"
    }
  }

  "forUpdate" in {
    val q = quote {
      query[TestEntity].filter(t => t.s == "a").forUpdate
    }
    testContext.run(q).string mustEqual "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t WHERE t.s = 'a' FOR UPDATE"
  }
}
