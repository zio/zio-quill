package io.getquill.context.sql.idiom

import io.getquill.Literal
import io.getquill.SqlMirrorContext
import io.getquill.SqliteDialect
import io.getquill.TestEntities

class SqliteDialectSpec extends OnConflictSpec {

  val ctx = new SqlMirrorContext(SqliteDialect, Literal) with TestEntities
  import ctx._

  "sortBy doesn't specify nulls ordering" - {
    "ascNullsFirst" in {
      ctx.run(qr1.sortBy(_.i)(Ord.ascNullsFirst)).string mustEqual
        "SELECT x1.s, x1.i, x1.l, x1.o FROM TestEntity x1 ORDER BY x1.i ASC /* NULLS FIRST omitted (not supported by sqlite) */"
    }
    "ascNullsLast" in {
      ctx.run(qr1.sortBy(_.i)(Ord.ascNullsLast)).string mustEqual
        "SELECT x2.s, x2.i, x2.l, x2.o FROM TestEntity x2 ORDER BY x2.i ASC /* NULLS LAST omitted (not supported by sqlite) */"
    }
    "descNullsFirst" in {
      ctx.run(qr1.sortBy(_.i)(Ord.descNullsFirst)).string mustEqual
        "SELECT x3.s, x3.i, x3.l, x3.o FROM TestEntity x3 ORDER BY x3.i DESC /* NULLS FIRST omitted (not supported by sqlite) */"
    }
    "descNullsLast" in {
      ctx.run(qr1.sortBy(_.i)(Ord.descNullsLast)).string mustEqual
        "SELECT x4.s, x4.i, x4.l, x4.o FROM TestEntity x4 ORDER BY x4.i DESC /* NULLS LAST omitted (not supported by sqlite) */"
    }
  }

  "transforms boolean literals into 0/1" in {
    ctx.run(qr1.map(t => (true, false))).string mustEqual
      "SELECT 1, 0 FROM TestEntity t"
  }

  "OnConflict" - {
    "no target - ignore" in {
      ctx.run(`no target - ignore`).string mustEqual
        "INSERT INTO TestEntity AS t (s,i,l,o) VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING"
    }
    "cols target - ignore" in {
      ctx.run(`cols target - ignore`).string mustEqual
        "INSERT INTO TestEntity (s,i,l,o) VALUES (?, ?, ?, ?) ON CONFLICT (i) DO NOTHING"
    }
    "no target - update" in {
      intercept[IllegalStateException] {
        ctx.run(`no target - update`.dynamic)
      }
    }
    "cols target - update" in {
      ctx.run(`cols target - update`).string mustEqual
        "INSERT INTO TestEntity AS t (s,i,l,o) VALUES (?, ?, ?, ?) ON CONFLICT (i,s) DO UPDATE SET l = ((t.l + EXCLUDED.l) / 2), s = EXCLUDED.s"
    }
  }
}
