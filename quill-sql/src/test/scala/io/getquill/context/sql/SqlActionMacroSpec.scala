package io.getquill.context.sql

import io.getquill.Spec
import io.getquill.context.mirror.Row
import io.getquill.context.sql.testContext.TestEntity
import io.getquill.context.sql.testContext.qr1
import io.getquill.context.sql.testContext.quote
import io.getquill.context.sql.testContext.unquote

class SqlActionMacroSpec extends Spec {

  "runs actions" - {
    "without bindings" - {
      "update" in {
        val q = quote {
          qr1.filter(t => t.s == null).update(_.s -> "s")
        }
        testContext.run(q).sql mustEqual
          "UPDATE TestEntity SET s = 's' WHERE s IS NULL"
      }
      "insert" in {
        val q = quote {
          qr1.insert(_.s -> "s")
        }
        testContext.run(q).sql mustEqual
          "INSERT INTO TestEntity (s) VALUES ('s')"
      }
      "delete" in {
        val q = quote {
          qr1.filter(t => t.s == null).delete
        }
        testContext.run(q).sql mustEqual
          "DELETE FROM TestEntity WHERE s IS NULL"
      }
    }
    "with bindings" - {
      "one" in {
        val q = quote {
          (s: String) => qr1.insert(_.s -> s)
        }
        val mirror = testContext.run(q)(List("s"))
        mirror.sql mustEqual "INSERT INTO TestEntity (s) VALUES (?)"
        mirror.bindList mustEqual List(Row("s"))
      }
      "two" in {
        val q = quote {
          (s: String, i: Int) => qr1.insert(_.s -> s, _.i -> i)
        }
        val mirror = testContext.run(q)(List(("s", 1)))
        mirror.sql mustEqual "INSERT INTO TestEntity (s,i) VALUES (?, ?)"
        mirror.bindList mustEqual List(Row("s", 1))
      }
    }
    "with returning" in {
      val q = quote {
        qr1.insert.returning(_.l)
      }
      val a = TestEntity
      val mirror = testContext.run(q)(List(TestEntity("s", 0, 1L, None)))
      mirror.sql mustEqual "INSERT INTO TestEntity (s,i,o) VALUES (?, ?, ?)"
      mirror.generated mustEqual Some("l")
    }
    "with assigned values and returning" in {
      val q = quote {
        qr1.insert(_.s -> "s", _.i -> 0).returning(_.l)
      }
      val a = TestEntity
      val mirror = testContext.run(q)
      mirror.sql mustEqual "INSERT INTO TestEntity (s,i) VALUES ('s', 0)"
      mirror.generated mustEqual Some("l")

    }
  }
}
