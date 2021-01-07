package io.getquill.context.sql.idiom

import io.getquill._
import io.getquill.idiom.StringToken

class MySQLDialectSpec extends OnConflictSpec {

  val ctx = new SqlMirrorContext(MySQLDialect, Literal) with TestEntities
  import ctx._

  "workaround for offset without limit" in {
    val q = quote {
      qr1.drop(1).map(t => t.s)
    }
    ctx.run(q).string mustEqual
      "SELECT t.s FROM TestEntity t LIMIT 18446744073709551610 OFFSET 1"
  }

  "uses CONCAT instead of ||" in {
    val q = quote {
      qr1.map(t => t.s + t.s)
    }
    ctx.run(q).string mustEqual
      "SELECT CONCAT(t.s, t.s) FROM TestEntity t"
  }

  "supports the `prepare` statement" in {
    val sql = s"test"
    MySQLDialect.prepareForProbing(sql) mustEqual
      s"PREPARE p${StringToken(sql.hashCode.abs.toString)} FROM '$sql'"
  }

  "workaround missing nulls ordering feature in mysql" - {
    "asc" in {
      val q = quote {
        qr1.sortBy(t => t.s)(Ord.asc)
      }
      ctx.run(q).string mustEqual
        "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t ORDER BY t.s ASC"
    }
    "desc" in {
      val q = quote {
        qr1.sortBy(t => t.s)(Ord.desc)
      }
      ctx.run(q).string mustEqual
        "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t ORDER BY t.s DESC"
    }
    "ascNullsFirst" in {
      val q = quote {
        qr1.sortBy(t => t.s)(Ord.ascNullsFirst)
      }
      ctx.run(q).string mustEqual
        "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t ORDER BY t.s ASC"
    }
    "descNullsFirst" in {
      val q = quote {
        qr1.sortBy(t => t.s)(Ord.descNullsFirst)
      }
      ctx.run(q).string mustEqual
        "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t ORDER BY ISNULL(t.s) DESC, t.s DESC"
    }
    "ascNullsLast" in {
      val q = quote {
        qr1.sortBy(t => t.s)(Ord.ascNullsLast)
      }
      ctx.run(q).string mustEqual
        "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t ORDER BY ISNULL(t.s) ASC, t.s ASC"
    }
    "descNullsLast" in {
      val q = quote {
        qr1.sortBy(t => t.s)(Ord.descNullsLast)
      }
      ctx.run(q).string mustEqual
        "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t ORDER BY t.s DESC"
    }
  }

  "Insert with returning generated with single column table" in {
    val q = quote {
      qr4.insert(lift(TestEntity4(0))).returningGenerated(_.i)
    }
    ctx.run(q).string mustEqual
      "INSERT INTO TestEntity4 (i) VALUES (DEFAULT)"
  }
  "Insert with returning generated - multiple fields - should not compile" in {
    val q = quote {
      qr1.insert(lift(TestEntity("s", 1, 2L, Some(3), true)))
    }
    "ctx.run(q.returningGenerated(r => (r.i, r.l))).string" mustNot compile
  }
  "Insert with returning should not compile" in {
    val q = quote {
      qr4.insert(lift(TestEntity4(0)))
    }
    "ctx.run(q.returning(_.i)).string" mustNot compile
  }

  "OnConflict" - {
    "no target - ignore" in {
      ctx.run(`no target - ignore`.dynamic).string mustEqual
        "INSERT IGNORE INTO TestEntity (s,i,l,o,b) VALUES (?, ?, ?, ?, ?)"

    }
    "cols target - ignore" in {
      ctx.run(`cols target - ignore`.dynamic).string mustEqual
        "INSERT INTO TestEntity (s,i,l,o,b) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE i=i"
    }
    "no target - update" in {
      ctx.run(`no target - update`).string mustEqual
        "INSERT INTO TestEntity (s,i,l,o,b) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE l = ((l + VALUES(l)) / 2), s = VALUES(s)"
    }
    "cols target - update" in {
      intercept[IllegalStateException] {
        ctx.run(`cols target - update`.dynamic)
      }
    }
  }
}
