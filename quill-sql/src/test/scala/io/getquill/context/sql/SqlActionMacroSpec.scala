package io.getquill.context.sql

import io.getquill._
import io.getquill.context.mirror.Row

class SqlActionMacroSpec extends Spec {

  "runs actions" - {
    import testContext._
    "without bindings" - {
      "update" in {
        val q = quote {
          qr1.filter(t => t.s == null).update(_.s -> "s")
        }
        testContext.run(q).string mustEqual
          "UPDATE TestEntity SET s = 's' WHERE s IS NULL"
      }
      "insert" in {
        val q = quote {
          qr1.insert(_.s -> "s")
        }
        testContext.run(q).string mustEqual
          "INSERT INTO TestEntity (s) VALUES ('s')"
      }
      "delete" in {
        val q = quote {
          qr1.filter(t => t.s == null).delete
        }
        testContext.run(q).string mustEqual
          "DELETE FROM TestEntity WHERE s IS NULL"
      }
    }
    "with bindings" - {
      "one" in {
        val q = quote {
          qr1.insert(_.s -> lift("s"))
        }
        val mirror = testContext.run(q)
        mirror.string mustEqual "INSERT INTO TestEntity (s) VALUES (?)"
        mirror.prepareRow mustEqual Row("s")
      }
      "two" in {
        val q = quote {
          qr1.insert(_.s -> lift("s"), _.i -> lift(1))
        }
        val mirror = testContext.run(q)
        mirror.string mustEqual "INSERT INTO TestEntity (s,i) VALUES (?, ?)"
        mirror.prepareRow mustEqual Row("s", 1)
      }
    }
    "with returning" in {
      val q = quote {
        qr1.insert(lift(TestEntity("s", 0, 1L, None))).returning(_.l)
      }
      val a = TestEntity
      val mirror = testContext.run(q)
      mirror.string mustEqual "INSERT INTO TestEntity (s,i,o) VALUES (?, ?, ?)"
      mirror.returningColumn mustEqual "l"
    }
    "with assigned values and returning" in {
      val q = quote {
        qr1.insert(_.s -> "s", _.i -> 0).returning(_.l)
      }
      val a = TestEntity
      val mirror = testContext.run(q)
      mirror.string mustEqual "INSERT INTO TestEntity (s,i) VALUES ('s', 0)"
      mirror.returningColumn mustEqual "l"
    }
  }
  "apply naming strategy to returning action" in testContext.withNaming(SnakeCase) { ctx =>
    import ctx._
    case class TestEntity4(intId: Int, textCol: String)
    val q = quote {
      query[TestEntity4].insert(lift(TestEntity4(1, "s"))).returning(_.intId)
    }
    val mirror = ctx.run(q)
    mirror.string mustEqual "INSERT INTO test_entity4 (text_col) VALUES (?)"
    mirror.returningColumn mustEqual "int_id"
  }
}
