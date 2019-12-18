package io.getquill.context.sql.idiom

import io.getquill.{ H2Dialect, Literal, SqlMirrorContext, TestEntities }

class H2DialectSpec extends OnConflictSpec {
  val ctx = new SqlMirrorContext(H2Dialect, Literal) with TestEntities
  import ctx._
  "OnConflict" - {
    "no target - ignore" in {
      ctx.run(`no target - ignore`).string mustEqual
        "INSERT INTO TestEntity (s,i,l,o) VALUES ($1, $2, $3, $4) ON CONFLICT DO NOTHING"
    }
    "no target - ignore batch" in {
      ctx.run(`no target - ignore batch`).groups.foreach {
        _._1 mustEqual
          "INSERT INTO TestEntity (s,i,l,o) VALUES ($1, $2, $3, $4) ON CONFLICT DO NOTHING"
      }
    }
  }
}
