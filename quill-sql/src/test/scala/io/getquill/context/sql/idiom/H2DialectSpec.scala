package io.getquill.context.sql.idiom

import io.getquill.{H2Dialect, Literal, SqlMirrorContext, TestEntities}

class H2DialectSpec extends OnConflictSpec {
  val ctx: SqlMirrorContext[H2Dialect.type,Literal.type] with TestEntities = new SqlMirrorContext(H2Dialect, Literal) with TestEntities
  import ctx._
  "OnConflict" - `onConflict with all` { i =>
    "no target - ignore" in {
      ctx.run(`no target - ignore`(i)).string mustEqual
        "INSERT INTO TestEntity (s,i,l,o,b) VALUES ($1, $2, $3, $4, $5) ON CONFLICT DO NOTHING"
    }
    "no target - ignore batch" in {
      ctx.run(`no target - ignore batch`).groups.foreach {
        _._1 mustEqual
          "INSERT INTO TestEntity (s,i,l,o,b) VALUES ($1, $2, $3, $4, $5) ON CONFLICT DO NOTHING"
      }
    }
  }
}
