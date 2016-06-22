package io.getquill.context.sql

import mirrorContext._
import io.getquill.context.mirror.Row

class SqlActionMacroSpec extends SqlSpec {

  "runs actions" - {
    "without bindings" - {
      "update" in {
        val q = quote {
          qr1.filter(t => t.s == null).update(_.s -> "s")
        }
        mirrorContext.run(q).sql mustEqual
          "UPDATE TestEntity SET s = 's' WHERE s IS NULL"
      }
      "insert" in {
        val q = quote {
          qr1.insert(_.s -> "s")
        }
        mirrorContext.run(q).sql mustEqual
          "INSERT INTO TestEntity (s) VALUES ('s')"
      }
      "delete" in {
        val q = quote {
          qr1.filter(t => t.s == null).delete
        }
        mirrorContext.run(q).sql mustEqual
          "DELETE FROM TestEntity WHERE s IS NULL"
      }
    }
    "with bindings" - {
      "one" in {
        val q = quote {
          (s: String) => qr1.insert(_.s -> s)
        }
        val mirror = mirrorContext.run(q)(List("s"))
        mirror.sql mustEqual "INSERT INTO TestEntity (s) VALUES (?)"
        mirror.bindList mustEqual List(Row("s"))
      }
      "two" in {
        val q = quote {
          (s: String, i: Int) => qr1.insert(_.s -> s, _.i -> i)
        }
        val mirror = mirrorContext.run(q)(List(("s", 1)))
        mirror.sql mustEqual "INSERT INTO TestEntity (s,i) VALUES (?, ?)"
        mirror.bindList mustEqual List(Row("s", 1))
      }
    }
    "with generated values" in {
      val q = quote {
        query[TestEntity].schema(_.generated(_.i)).insert
      }
      val mirror = mirrorContext.run(q)(List(TestEntity("s", 0, 1L, None)))
      mirror.sql mustEqual "INSERT INTO TestEntity (s,l,o) VALUES (?, ?, ?)"
      mirror.generated mustEqual Some("i")
    }
  }
}
