package io.getquill.context.sql.idiom

import io.getquill.ReturnAction.ReturnColumns
import io.getquill.{ MirrorSqlDialectWithReturnMulti, Spec }
import io.getquill.context.mirror.Row
import io.getquill.context.sql.testContext
import io.getquill.context.sql.testContext._

class SqlActionSpec extends Spec {

  case class TwoIntsClassScope(one: Int, two: Int)

  // remove the === matcher from scalatest so that we can test === in Context.extra
  override def convertToEqualizer[T](left: T): Equalizer[T] = new Equalizer(left)

  "shows the sql representation of normalized actions" - {
    "action" - {
      "insert" - {
        "not affected by variable name" - {
          "simple" in {
            val q = quote { (v: TestEntity) =>
              query[TestEntity].insertValue(v)
            }
            val v = TestEntity("s", 1, 2L, Some(1), true)
            testContext.run(q(lift(v))).string mustEqual "INSERT INTO TestEntity (s,i,l,o,b) VALUES (?, ?, ?, ?, ?)"
          }
          "returning" in testContext.withDialect(MirrorSqlDialectWithReturnMulti) { ctx =>
            import ctx._
            val q = quote { (v: TestEntity) =>
              query[TestEntity].insertValue(v)
            }
            val v = TestEntity("s", 1, 2L, Some(1), true)
            ctx.run(q(lift(v)).returning(v => v.i)).string mustEqual "INSERT INTO TestEntity (s,i,l,o,b) VALUES (?, ?, ?, ?, ?)"
          }
          "returning generated" in {
            val q = quote { (v: TestEntity) =>
              query[TestEntity].insertValue(v)
            }
            val v = TestEntity("s", 1, 2L, Some(1), true)
            testContext.run(q(lift(v)).returningGenerated(v => v.i)).string mustEqual "INSERT INTO TestEntity (s,l,o,b) VALUES (?, ?, ?, ?)"
          }
          "foreach" in {
            val v = TestEntity("s", 1, 2L, Some(1), true)
            testContext.run(
              liftQuery(List(v)).foreach(v => query[TestEntity].insertValue(v))
            ).groups mustEqual List(("INSERT INTO TestEntity (s,i,l,o,b) VALUES (?, ?, ?, ?, ?)", List(Row(v.productIterator.toList: _*))))
          }
          "foreach returning" in testContext.withDialect(MirrorSqlDialectWithReturnMulti) { ctx =>
            import ctx._
            val v = TestEntity("s", 1, 2L, Some(1), true)
            ctx.run(liftQuery(List(v)).foreach(v => query[TestEntity].insertValue(v).returning(v => v.i))).groups mustEqual
              List(("INSERT INTO TestEntity (s,i,l,o,b) VALUES (?, ?, ?, ?, ?)",
                ReturnColumns(List("i")),
                List(Row(v.productIterator.toList: _*))
              ))
          }
          "foreach returning generated" in {
            val v = TestEntity("s", 1, 2L, Some(1), true)
            testContext.run(
              liftQuery(List(v)).foreach(v => query[TestEntity].insertValue(v).returningGenerated(v => v.i))
            ).groups mustEqual
              List(("INSERT INTO TestEntity (s,l,o,b) VALUES (?, ?, ?, ?)",
                ReturnColumns(List("i")),
                List(Row(v.productIterator.toList.filter(m => !m.isInstanceOf[Int]): _*))
              ))
          }
        }
        "simple" in {
          val q = quote {
            qr1.insert(_.i -> 1, _.s -> "s")
          }
          testContext.run(q).string mustEqual
            "INSERT INTO TestEntity (i,s) VALUES (1, 's')"
        }
        "using nested select" in {
          val q = quote {
            qr1.insert(_.l -> qr2.map(t => t.i).size, _.s -> "s")
          }
          testContext.run(q).string mustEqual
            "INSERT INTO TestEntity (l,s) VALUES ((SELECT COUNT(t.i) FROM TestEntity2 t), 's')"
        }
        "returning" in testContext.withDialect(MirrorSqlDialectWithReturnMulti) { ctx =>
          import ctx._
          val q = quote {
            query[TestEntity].insertValue(lift(TestEntity("s", 1, 2L, Some(1), true))).returning(_.l)
          }
          val run = ctx.run(q).string mustEqual
            "INSERT INTO TestEntity (s,i,l,o,b) VALUES (?, ?, ?, ?, ?)"
        }
        "returning generated" in {
          val q = quote {
            query[TestEntity].insertValue(lift(TestEntity("s", 1, 2L, Some(1), true))).returningGenerated(_.l)
          }
          val run = testContext.run(q).string mustEqual
            "INSERT INTO TestEntity (s,i,o,b) VALUES (?, ?, ?, ?)"
        }
        "returning with single column table" in testContext.withDialect(MirrorSqlDialectWithReturnMulti) { ctx =>
          import ctx._
          val q = quote {
            qr4.insertValue(lift(TestEntity4(0))).returning(_.i)
          }
          ctx.run(q).string mustEqual
            "INSERT INTO TestEntity4 (i) VALUES (?)"
        }
        "returning generated with single column table" in {
          val q = quote {
            qr4.insertValue(lift(TestEntity4(0))).returningGenerated(_.i)
          }
          testContext.run(q).string mustEqual
            "INSERT INTO TestEntity4 DEFAULT VALUES"
        }
      }
      "update" - {
        "with filter" in {
          val q = quote {
            qr1.filter(t => t.s == null).update(_.s -> "s")
          }
          testContext.run(q).string mustEqual
            "UPDATE TestEntity AS t SET s = 's' WHERE t.s IS NULL"
        }
        "without filter" in {
          val q = quote {
            qr1.update(_.s -> "s")
          }
          testContext.run(q).string mustEqual
            "UPDATE TestEntity SET s = 's'"
        }
        "using a table column" in {
          val q = quote {
            qr1.update(t => t.i -> (t.i + 1))
          }
          testContext.run(q).string mustEqual
            "UPDATE TestEntity SET i = (i + 1)"
        }
        "using nested select" in {
          val q = quote {
            qr1.update(_.l -> qr2.map(t => t.i).size)
          }
          testContext.run(q).string mustEqual
            "UPDATE TestEntity SET l = (SELECT COUNT(t.i) FROM TestEntity2 t)"
        }
        "using co-related query" in {
          val q = quote {
            qr1.filter(t => qr2.filter(tt => tt.i == t.i).nonEmpty).update(_.l -> 123)
          }
          testContext.run(q).string mustEqual
            "UPDATE TestEntity AS t SET l = 123 WHERE EXISTS (SELECT tt.s, tt.i, tt.l, tt.o FROM TestEntity2 tt WHERE tt.i = t.i)"
        }
      }
      "delete" - {
        "with filter" in {
          val q = quote {
            qr1.filter(t => t.s == null).delete
          }
          testContext.run(q).string mustEqual
            "DELETE FROM TestEntity AS t WHERE t.s IS NULL"
        }
        "without filter" in {
          val q = quote {
            qr1.delete
          }
          testContext.run(q).string mustEqual
            "DELETE FROM TestEntity"
        }
      }
    }
  }
}
