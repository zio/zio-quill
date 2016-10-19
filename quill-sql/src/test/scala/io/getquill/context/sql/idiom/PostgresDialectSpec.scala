package io.getquill.context.sql.idiom

import io.getquill.Spec
import io.getquill.PostgresDialect
import io.getquill.SqlMirrorContext
import io.getquill.Literal
import io.getquill.TestEntities

class PostgresDialectSpec extends Spec {

  val context = new SqlMirrorContext[PostgresDialect, Literal] with TestEntities
  import context._

  "applies explicit casts" - {
    "toLong" in {
      val q = quote {
        qr1.map(t => t.s.toLong)
      }
      context.run(q).string mustEqual "SELECT t.s::bigint FROM TestEntity t"
    }
    "toInt" in {
      val q = quote {
        qr1.map(t => t.s.toInt)
      }
      context.run(q).string mustEqual "SELECT t.s::integer FROM TestEntity t"
    }
    "upsert" in {
      val e = TestEntity("", 1, 1L, Some(1))
      val q = quote {
        query[TestEntity].upsert(lift(e)).conflict(_.i).conflictUpdate(_.i -> lift(2), _.l -> lift(1L), _.s -> lift("Test String"))
      }

      println(context.run(q).string)
    }
  }
}
