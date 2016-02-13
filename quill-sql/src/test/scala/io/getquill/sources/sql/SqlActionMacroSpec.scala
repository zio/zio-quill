package io.getquill.sources.sql

import io.getquill._
import io.getquill.Spec
import io.getquill.sources.mirror.Row

class SqlActionMacroSpec extends Spec {

  "runs actions" - {
    "without bindings" - {
      "update" in {
        val q = quote {
          qr1.filter(t => t.s == null).update(_.s -> "s")
        }
        mirrorSource.run(q).sql mustEqual
          "UPDATE TestEntity SET s = 's' WHERE s IS NULL"
      }
      "insert" in {
        val q = quote {
          qr1.insert(_.s -> "s")
        }
        mirrorSource.run(q).sql mustEqual
          "INSERT INTO TestEntity (s) VALUES ('s')"
      }
      "delete" in {
        val q = quote {
          qr1.filter(t => t.s == null).delete
        }
        mirrorSource.run(q).sql mustEqual
          "DELETE FROM TestEntity WHERE s IS NULL"
      }
    }
    "with bindings" - {
      "one" in {
        val q = quote {
          (s: String) => qr1.insert(_.s -> s)
        }
        val mirror = mirrorSource.run(q)(List("s"))
        mirror.sql mustEqual "INSERT INTO TestEntity (s) VALUES (?)"
        mirror.bindList mustEqual List(Row("s"))
      }
      "two" in {
        val q = quote {
          (s: String, i: Int) => qr1.insert(_.s -> s, _.i -> i)
        }
        val mirror = mirrorSource.run(q)(List(("s", 1)))
        mirror.sql mustEqual "INSERT INTO TestEntity (s,i) VALUES (?, ?)"
        mirror.bindList mustEqual List(Row("s", 1))
      }
    }
  }
}
