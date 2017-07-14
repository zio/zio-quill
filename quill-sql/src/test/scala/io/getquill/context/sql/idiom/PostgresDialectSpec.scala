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
        query[TestEntity]
          .upsert(e)
          .conflict(_.i)
          .conflictUpdate(_.i -> lift(2), _.l -> lift(1L), _.s -> lift("Test String"))
      }

      val q2 = quote {
        query[TestEntity]
          .upsert(lift(e))
          .conflict(_.i)
          .doNothing()
      }

      val q3 = quote {
        query[TestEntity]
          .upsert(_.s -> "Hi", _.l -> 10L)
          .conflict(_.i)
          .conflictUpdate(_.s -> "Hihi")
      }

      println(context.run(q).string)
      println(context.run(q2).string)
      println(context.run(q3).string)

      /*
      INSERT INTO TestEntity (s, l, o) VALUES (s, l, o) ON CONFLICT(i) DO UPDATE SET i = ?, l = ?, s = ?
      INSERT INTO TestEntity (s, l, o) VALUES (?, ?, ?) ON CONFLICT(i) DO NOTHING
      INSERT INTO TestEntity (s, l) VALUES ('Hi', 10) ON CONFLICT(i) DO UPDATE SET s = 'Yoyo'
      */
    }
  }
}
