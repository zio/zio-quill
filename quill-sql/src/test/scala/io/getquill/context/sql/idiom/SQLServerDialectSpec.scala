package io.getquill.context.sql.idiom

import io.getquill._
import io.getquill.idiom.StringToken

class SQLServerDialectSpec extends Spec {

  "emptySetContainsToken" in {
    SQLServerDialect.emptySetContainsToken(StringToken("w/e")) mustBe StringToken("1 <> 1")
  }

  val ctx = new SqlMirrorContext(SQLServerDialect, Literal) with TestEntities
  import ctx._

  "uses + instead of ||" in {
    val q = quote {
      qr1.map(t => t.s + t.s)
    }
    ctx.run(q).string mustEqual
      "SELECT t.s + t.s FROM TestEntity t"
  }

  "top" in {
    val q = quote {
      qr1.take(15).map(t => t.i)
    }
    ctx.run(q).string mustEqual
      "SELECT TOP 15 t.i FROM TestEntity t"
  }

  "literal booleans" - {
    "uses 1=1 instead of true" in {
      ctx.run(qr4.filter(t => true)).string mustEqual
        "SELECT t.i FROM TestEntity4 t WHERE 1=1"
    }

    "uses 1=0 instead of false" in {
      ctx.run(qr4.filter(t => false)).string mustEqual
        "SELECT t.i FROM TestEntity4 t WHERE 1=0"
    }
  }

  "offset/fetch" - {

    val withOrd = quote {
      qr1.sortBy(t => t.i)(Ord.desc).map(_.s)
    }

    def offset[T](q: Quoted[Query[T]]) = quote(q.drop(1))
    def offsetFetch[T](q: Quoted[Query[T]]) = quote(q.drop(2).take(3))

    "offset" in {
      ctx.run(offset(withOrd)).string mustEqual
        "SELECT t.s FROM TestEntity t ORDER BY t.i DESC OFFSET 1 ROWS"
    }

    "offset with fetch " in {
      ctx.run(offsetFetch(withOrd)).string mustEqual
        "SELECT t.s FROM TestEntity t ORDER BY t.i DESC OFFSET 2 ROWS FETCH FIRST 3 ROWS ONLY"
    }

    "fail without ordering" in {
      intercept[IllegalStateException] {
        ctx.run(offset(qr1))
      }.getMessage mustEqual "SQLServer does not support OFFSET without ORDER BY"

      intercept[IllegalStateException] {
        ctx.run(offsetFetch(qr1))
      }.getMessage mustEqual "SQLServer does not support OFFSET without ORDER BY"
    }
  }

  "Insert with returning via OUTPUT" - {
    "returning" - {
      "with single column table" in {
        val q = quote {
          qr4.insert(lift(TestEntity4(0))).returning(_.i)
        }
        ctx.run(q).string mustEqual
          "INSERT INTO TestEntity4 (i) OUTPUT INSERTED.i VALUES (?)"
      }
      "with multi column table" in {
        val q = quote {
          qr1.insert(lift(TestEntity("s", 0, 0L, Some(3)))).returning(r => (r.i, r.l))
        }
        ctx.run(q).string mustEqual
          "INSERT INTO TestEntity (s,i,l,o) OUTPUT INSERTED.i, INSERTED.l VALUES (?, ?, ?, ?)"
      }
      "with multiple fields + operations" in {
        val q = quote {
          qr1.insert(lift(TestEntity("s", 1, 2L, Some(3)))).returning(r => (r.i, r.l + 1))
        }
        ctx.run(q).string mustEqual
          "INSERT INTO TestEntity (s,i,l,o) OUTPUT INSERTED.i, INSERTED.l + 1 VALUES (?, ?, ?, ?)"
      }
      "with query - should not compile" in {
        """import ctx._; quote { qr1.insert(lift(TestEntity("s", 1, 2L, Some(3)))).returning(r => query[TestEntity].filter(t => t.i == r.i)) }""" mustNot compile
      }
    }

    "returningGenerated" - {
      "returning generated with single column table" in {
        val q = quote {
          qr4.insert(lift(TestEntity4(0))).returningGenerated(_.i)
        }
        ctx.run(q).string mustEqual
          "INSERT INTO TestEntity4 OUTPUT INSERTED.i DEFAULT VALUES"
      }
      "with multi column table" in {
        val q = quote {
          qr1.insert(lift(TestEntity("s", 0, 0L, Some(3)))).returningGenerated(r => (r.i, r.l))
        }
        ctx.run(q).string mustEqual
          "INSERT INTO TestEntity (s,o) OUTPUT INSERTED.i, INSERTED.l VALUES (?, ?)"
      }
      "with multiple fields + operations" in {
        val q = quote {
          qr1.insert(lift(TestEntity("s", 0, 0L, Some(3)))).returningGenerated(r => (r.i, r.l + 1))
        }
        ctx.run(q).string mustEqual
          "INSERT INTO TestEntity (s,o) OUTPUT INSERTED.i, INSERTED.l + 1 VALUES (?, ?)"
      }
      "with query - should not compile" in {
        """import ctx._; quote { qr1.insert(lift(TestEntity("s", 0, 0L, Some(3)))).returningGenerated(r => query[TestEntity].filter(t => t.i == r.i)) }""" mustNot compile
      }
    }
  }
}
