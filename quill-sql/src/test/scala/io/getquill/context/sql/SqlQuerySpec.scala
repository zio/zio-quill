package io.getquill.context.sql

import io.getquill.Spec
import io.getquill.context.sql.testContext._
import io.getquill.Literal

class SqlQuerySpec extends Spec {

  implicit val naming = new Literal {}

  "transforms the ast into a flatten sql-like structure" - {

    "inner join query" in {
      val q = quote {
        for {
          a <- qr1
          b <- qr2 if (a.s != null && b.i > a.i)
        } yield {
          (a.i, b.i)
        }
      }
      testContext.run(q).string mustEqual
        "SELECT a.i, b.i FROM TestEntity a, TestEntity2 b WHERE (a.s IS NOT NULL) AND (b.i > a.i)"
    }

    "outer join query" in {
      val q = quote {
        qr1.leftJoin(qr2).on((a, b) => a.s != null && b.i > a.i)
      }
      testContext.run(q).string mustEqual
        "SELECT a.s, a.i, a.l, a.o, b.s, b.i, b.l, b.o FROM TestEntity a LEFT JOIN TestEntity2 b ON (a.s IS NOT NULL) AND (b.i > a.i)"
    }

    "flat outer join" in {
      val q = quote {
        for {
          e1 <- qr1
          e2 <- qr2.leftJoin(e2 => e2.i == e1.i)
        } yield (e1.i, e2.map(e => e.i))
      }
      testContext.run(q.dynamic).string mustEqual
        "SELECT e1.i, e2.i FROM TestEntity e1 LEFT JOIN TestEntity2 e2 ON e2.i = e1.i"
    }

    "value query" - {
      "operation" in {
        val q = quote {
          qr1.map(t => t.i).contains(1)
        }
        testContext.run(q).string mustEqual
          "SELECT 1 IN (SELECT t.i FROM TestEntity t)"
      }
      "simple value" in {
        val q = quote(1)
        testContext.run(q).string mustEqual
          "SELECT 1"
      }
    }

    "raw queries with infix" - {
      "using tuples" in {
        val q = quote {
          infix"""SELECT t.s AS "_1", t.i AS "_2" FROM TestEntity t""".as[Query[(String, Int)]]
        }
        testContext.run(q).string mustEqual
          """SELECT x._1, x._2 FROM (SELECT t.s AS "_1", t.i AS "_2" FROM TestEntity t) x"""
      }
      "using single value" in {
        val q = quote {
          infix"""SELECT t.i FROM TestEntity t""".as[Query[Int]]
        }
        testContext.run(q).string mustEqual
          """SELECT x.* FROM (SELECT t.i FROM TestEntity t) x"""
      }
    }

    "nested infix query" - {
      "as source" in {
        val q = quote {
          infix"SELECT * FROM TestEntity".as[Query[TestEntity]].filter(t => t.i == 1)
        }
        testContext.run(q).string mustEqual
          "SELECT t.s, t.i, t.l, t.o FROM (SELECT * FROM TestEntity) t WHERE t.i = 1"
      }
      "fails if used as the flatMap body" in {
        val q = quote {
          qr1.flatMap(a => infix"SELECT * FROM TestEntity2 t where t.s = ${a.s}".as[Query[TestEntity2]])
        }
        val e = intercept[IllegalStateException] {
          SqlQuery(q.ast)
        }
      }
    }
    "sorted query" - {
      "with map" in {
        val q = quote {
          qr1.sortBy(t => t.s).map(t => t.s)
        }
        testContext.run(q).string mustEqual
          "SELECT t.s FROM TestEntity t ORDER BY t.s ASC NULLS FIRST"
      }
      "with filter" in {
        val q = quote {
          qr1.filter(t => t.s == "s").sortBy(t => t.s).map(t => (t.i))
        }
        testContext.run(q).string mustEqual
          "SELECT t.i FROM TestEntity t WHERE t.s = 's' ORDER BY t.s ASC NULLS FIRST"
      }
      "with outer filter" in {
        val q = quote {
          qr1.sortBy(t => t.s).filter(t => t.s == "s").map(t => t.s)
        }
        testContext.run(q).string mustEqual
          "SELECT t.s FROM TestEntity t WHERE t.s = 's' ORDER BY t.s ASC NULLS FIRST"
      }
      "with flatMap" in {
        val q = quote {
          qr1.sortBy(t => t.s).flatMap(t => qr2.map(t => t.s))
        }
        testContext.run(q).string mustEqual
          "SELECT t1.s FROM (SELECT t.* FROM TestEntity t ORDER BY t.s ASC NULLS FIRST) t, TestEntity2 t1"
      }
      "tuple criteria" - {
        "single ordering" in {
          val q = quote {
            qr1.sortBy(t => (t.s, t.i))(Ord.asc).map(t => t.s)
          }
          testContext.run(q).string mustEqual
            "SELECT t.s FROM TestEntity t ORDER BY t.s ASC, t.i ASC"
        }
        "ordering per column" in {
          val q = quote {
            qr1.sortBy(t => (t.s, t.i))(Ord(Ord.asc, Ord.desc)).map(t => t.s)
          }
          testContext.run(q).string mustEqual
            "SELECT t.s FROM TestEntity t ORDER BY t.s ASC, t.i DESC"
        }
      }
      "multiple sortBy" in {
        val q = quote {
          qr1.sortBy(t => (t.s, t.i)).sortBy(t => t.l).map(t => t.s)
        }
        testContext.run(q).string mustEqual
          "SELECT t.s FROM (SELECT t.l, t.s FROM TestEntity t ORDER BY t.s ASC NULLS FIRST, t.i ASC NULLS FIRST) t ORDER BY t.l ASC NULLS FIRST"
      }
      "expression" - {
        "neg" in {
          val q = quote {
            qr1.sortBy(t => -t.i)(Ord.desc)
          }
          testContext.run(q).string mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t ORDER BY - (t.i) DESC"
        }
        "add" in {
          val q = quote {
            qr1.sortBy(t => t.l - t.i)
          }
          testContext.run(q).string mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t ORDER BY (t.l - t.i) ASC NULLS FIRST"
        }
      }
      "fails if the sortBy criteria is malformed" in {
        case class Test(a: (Int, Int))
        implicit val o: Ordering[TestEntity] = null
        val q = quote {
          query[Test].sortBy(_.a)(Ord(Ord.asc, Ord.desc))
        }
        val e = intercept[IllegalStateException] {
          SqlQuery(q.ast)
        }
      }
    }
    "grouped query" - {
      "simple" in {
        val q = quote {
          qr1.groupBy(t => t.i).map(t => t._1)
        }
        testContext.run(q).string mustEqual
          "SELECT t.i FROM TestEntity t GROUP BY t.i"
      }
      "nested" in {
        val q = quote {
          qr1.groupBy(t => t.i).map(t => t._1).flatMap(t => qr2)
        }
        testContext.run(q).string mustEqual
          "SELECT x.s, x.i, x.l, x.o FROM (SELECT t.i FROM TestEntity t GROUP BY t.i) t, TestEntity2 x"
      }
      "without map" in {
        val q = quote {
          qr1.groupBy(t => t.i)
        }
        val e = intercept[IllegalStateException] {
          SqlQuery(q.ast)
        }
      }
      "tuple" in {
        val q = quote {
          qr1.groupBy(t => (t.i, t.l)).map(t => t._1)
        }
        testContext.run(q).string mustEqual
          "SELECT t._1, t._2 FROM (SELECT t.i _1, t.l _2 FROM TestEntity t GROUP BY t.i, t.l) t"
      }
      "aggregated" - {
        "simple" in {
          val q = quote {
            qr1.groupBy(t => t.i).map {
              case (i, entities) => (i, entities.size)
            }
          }
          testContext.run(q).string mustEqual
            "SELECT t._1, t._2 FROM (SELECT t.i _1, COUNT(*) _2 FROM TestEntity t GROUP BY t.i) t"
        }
        "mapped" in {
          val q = quote {
            qr1.groupBy(t => t.i).map {
              case (i, entities) => (i, entities.map(_.l).max)
            }
          }
          testContext.run(q).string mustEqual
            "SELECT t._1, t._2 FROM (SELECT t.i _1, MAX(t.l) _2 FROM TestEntity t GROUP BY t.i) t"
        }
      }
      "invalid groupby criteria" in {
        val q = quote {
          qr1.groupBy(t => t).map(t => t)
        }
        val e = intercept[IllegalStateException] {
          SqlQuery(q.ast)
        }
      }
    }
    "aggregated query" in {
      val q = quote {
        qr1.map(t => t.i).max
      }
      testContext.run(q).string mustEqual
        "SELECT MAX(t.i) FROM TestEntity t"
    }
    "distinct query" in {
      val q = quote {
        qr1.map(t => t.i).distinct
      }
      testContext.run(q).string mustEqual
        "SELECT DISTINCT t.i FROM TestEntity t"
    }
    "limited query" - {
      "simple" in {
        val q = quote {
          qr1.take(10)
        }
        testContext.run(q).string mustEqual
          "SELECT x.s, x.i, x.l, x.o FROM TestEntity x LIMIT 10"
      }
      "nested" in {
        val q = quote {
          qr1.take(10).flatMap(a => qr2)
        }
        testContext.run(q).string mustEqual
          "SELECT x.s, x.i, x.l, x.o FROM (SELECT x.* FROM TestEntity x LIMIT 10) a, TestEntity2 x"
      }
      "with map" in {
        val q = quote {
          qr1.take(10).map(t => t.s)
        }
        testContext.run(q).string mustEqual
          "SELECT t.s FROM TestEntity t LIMIT 10"
      }
      "multiple limits" in {
        val q = quote {
          qr1.take(1).take(10)
        }
        testContext.run(q).string mustEqual
          "SELECT x.s, x.i, x.l, x.o FROM (SELECT x.s, x.i, x.l, x.o FROM TestEntity x LIMIT 1) x LIMIT 10"
      }
    }
    "offset query" - {
      "simple" in {
        val q = quote {
          qr1.drop(10)
        }
        testContext.run(q).string mustEqual
          "SELECT x.s, x.i, x.l, x.o FROM TestEntity x OFFSET 10"
      }
      "nested" in {
        val q = quote {
          qr1.drop(10).flatMap(a => qr2)
        }
        testContext.run(q).string mustEqual
          "SELECT x.s, x.i, x.l, x.o FROM (SELECT x.* FROM TestEntity x OFFSET 10) a, TestEntity2 x"
      }
      "with map" in {
        val q = quote {
          qr1.drop(10).map(t => t.s)
        }
        testContext.run(q).string mustEqual
          "SELECT t.s FROM TestEntity t OFFSET 10"
      }
      "multiple offsets" in {
        val q = quote {
          qr1.drop(1).drop(10)
        }
        testContext.run(q).string mustEqual
          "SELECT x.s, x.i, x.l, x.o FROM (SELECT x.s, x.i, x.l, x.o FROM TestEntity x OFFSET 1) x OFFSET 10"
      }
    }
    "limited and offset query" - {
      "simple" in {
        val q = quote {
          qr1.drop(10).take(11)
        }
        testContext.run(q).string mustEqual
          "SELECT x.s, x.i, x.l, x.o FROM TestEntity x LIMIT 11 OFFSET 10"
      }
      "nested" in {
        val q = quote {
          qr1.drop(10).take(11).flatMap(a => qr2)
        }
        testContext.run(q).string mustEqual
          "SELECT x.s, x.i, x.l, x.o FROM (SELECT x.* FROM TestEntity x LIMIT 11 OFFSET 10) a, TestEntity2 x"
      }
      "multiple" in {
        val q = quote {
          qr1.drop(1).take(2).drop(3).take(4)
        }
        testContext.run(q).string mustEqual
          "SELECT x.s, x.i, x.l, x.o FROM (SELECT x.s, x.i, x.l, x.o FROM TestEntity x LIMIT 2 OFFSET 1) x LIMIT 4 OFFSET 3"
      }
      "take.drop" in {
        val q = quote {
          qr1.take(1).drop(2)
        }
        testContext.run(q).string mustEqual
          "SELECT x.s, x.i, x.l, x.o FROM (SELECT x.s, x.i, x.l, x.o FROM TestEntity x LIMIT 1) x OFFSET 2"
      }
      "for comprehension" - {
        val q = quote(for {
          q1 <- qr1
          q2 <- qr2 if q1.i == q2.i
        } yield (q1.i, q2.i, q1.s, q2.s))

        "take" in {
          testContext.run(q.take(3)).string mustEqual
            "SELECT q1.i, q2.i, q1.s, q2.s FROM TestEntity q1, TestEntity2 q2 WHERE q1.i = q2.i LIMIT 3"
        }
        "drop" in {
          testContext.run(q.drop(3)).string mustEqual
            "SELECT q1.i, q2.i, q1.s, q2.s FROM TestEntity q1, TestEntity2 q2 WHERE q1.i = q2.i OFFSET 3"
        }
      }
    }
    "set operation query" - {
      "union" in {
        val q = quote {
          qr1.union(qr1)
        }
        testContext.run(q).string mustEqual
          "SELECT x.s, x.i, x.l, x.o FROM ((SELECT x.s, x.i, x.l, x.o FROM TestEntity x) UNION (SELECT x.s, x.i, x.l, x.o FROM TestEntity x)) x"
      }
      "unionAll" in {
        val q = quote {
          qr1.unionAll(qr1)
        }
        testContext.run(q).string mustEqual
          "SELECT x.s, x.i, x.l, x.o FROM ((SELECT x.s, x.i, x.l, x.o FROM TestEntity x) UNION ALL (SELECT x.s, x.i, x.l, x.o FROM TestEntity x)) x"
      }
    }
    "unary operation query" - {
      "nonEmpty" in {
        val q = quote {
          qr1.nonEmpty
        }
        testContext.run(q).string mustEqual
          "SELECT EXISTS (SELECT x.* FROM TestEntity x)"
      }
      "isEmpty" in {
        val q = quote {
          qr1.isEmpty
        }
        testContext.run(q).string mustEqual
          "SELECT NOT EXISTS (SELECT x.* FROM TestEntity x)"
      }
    }
    "aggregated and mapped query" in {
      val q = quote {
        (for {
          q1 <- qr1
          q2 <- qr2
        } yield {
          q2.i
        }).min
      }
      testContext.run(q).string mustEqual
        "SELECT MIN(q2.i) FROM TestEntity q1, TestEntity2 q2"
    }
    "nested" - {
      "pointless nesting" in {
        val q = quote {
          qr1.nested
        }
        testContext.run(q).string mustEqual
          "SELECT x.s, x.i, x.l, x.o FROM (SELECT x.s, x.i, x.l, x.o FROM TestEntity x) x"
      }
      "pointless nesting of single yielding element" in {
        val q = quote {
          qr1.map(x => x.i).nested
        }
        testContext.run(q).string mustEqual "SELECT x.* FROM (SELECT x.i FROM TestEntity x) x"
      }
      "pointless nesting in for-comp of single yielding element" in {
        val q = quote {
          (for {
            a <- qr1
            b <- qr2
          } yield a.i).nested
        }
        testContext.run(q).string mustEqual "SELECT x.* FROM (SELECT a.i FROM TestEntity a, TestEntity2 b) x"
      }
      "mapped" in {
        val q = quote {
          qr1.nested.map(t => t.i)
        }
        testContext.run(q).string mustEqual
          "SELECT t.i FROM (SELECT x.i FROM TestEntity x) t"
      }
      "filter + map" in {
        val q = quote {
          qr1.filter(t => t.i == 1).nested.map(t => t.i)
        }
        testContext.run(q).string mustEqual
          "SELECT t.i FROM (SELECT t.i FROM TestEntity t WHERE t.i = 1) t"
      }
    }
  }
}
