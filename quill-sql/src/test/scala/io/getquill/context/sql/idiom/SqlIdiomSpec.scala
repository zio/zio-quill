package io.getquill.context.sql.idiom

import io.getquill.Spec
import io.getquill.context.sql.testContext
import io.getquill.context.sql.testContext._

class SqlIdiomSpec extends Spec {

  "shows the sql representation of normalized asts" - {
    "query" - {
      "without filter" in {
        testContext.run(qr1).string mustEqual
          "SELECT x.s, x.i, x.l, x.o FROM TestEntity x"
      }
      "with filter" in {
        val q = quote {
          qr1.filter(t => t.s == "s")
        }
        testContext.run(q).string mustEqual
          "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.s = 's'"
      }
      "multiple entities" in {
        val q = quote {
          for {
            a <- qr1
            b <- qr2 if (a.s == b.s)
          } yield {
            a.s
          }
        }
        testContext.run(q).string mustEqual
          "SELECT a.s FROM TestEntity a, TestEntity2 b WHERE a.s = b.s"
      }
      "distinct" - {
        "simple" in {
          val q = quote {
            qr1.distinct
          }
          testContext.run(q).string mustEqual
            "SELECT x.s, x.i, x.l, x.o FROM (SELECT DISTINCT x.s, x.i, x.l, x.o FROM TestEntity x) x"
        }

        "distinct single" in {
          val q = quote {
            qr1.map(i => i.i).distinct
          }
          testContext.run(q).string mustEqual
            "SELECT DISTINCT i.i FROM TestEntity i"
        }

        "distinct tuple" in {
          val q = quote {
            qr1.map(i => (i.i, i.l)).distinct
          }
          testContext.run(q).string mustEqual
            "SELECT DISTINCT i.i, i.l FROM TestEntity i"
        }

        "distinct nesting" in {
          val q = quote {
            qr1.map(i => i.i).distinct.map(x => x + 1)
          }
          testContext.run(q).string mustEqual
            "SELECT x + 1 FROM (SELECT DISTINCT i.i FROM TestEntity i) x"
        }
        "distinct followed by aggregation" in {
          val q = quote {
            qr1.map(i => i.i).distinct.size
          }
          testContext.run(q).string mustEqual
            "SELECT COUNT(*) FROM (SELECT DISTINCT i.i FROM TestEntity i) x"
        }
      }
      "sorted" - {
        "simple" in {
          val q = quote {
            qr1.filter(t => t.s != null).sortBy(_.s)
          }
          testContext.run(q).string mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.s IS NOT NULL ORDER BY t.s ASC NULLS FIRST"
        }
        "nested" in {
          val q = quote {
            for {
              a <- qr1.sortBy(t => t.s)
              b <- qr2.sortBy(t => t.i)
            } yield {
              (a.s, b.i)
            }
          }
          testContext.run(q).string mustEqual
            "SELECT t.s, t1.i FROM (SELECT t.s FROM TestEntity t ORDER BY t.s ASC NULLS FIRST) t, (SELECT t1.i FROM TestEntity2 t1 ORDER BY t1.i ASC NULLS FIRST) t1"
        }
        "asc" in {
          val q = quote {
            qr1.sortBy(t => t.s)(Ord.asc)
          }
          testContext.run(q).string mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t ORDER BY t.s ASC"
        }
        "desc" in {
          val q = quote {
            qr1.sortBy(t => t.s)(Ord.desc)
          }
          testContext.run(q).string mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t ORDER BY t.s DESC"
        }
        "ascNullsFirst" in {
          val q = quote {
            qr1.sortBy(t => t.s)(Ord.ascNullsFirst)
          }
          testContext.run(q).string mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t ORDER BY t.s ASC NULLS FIRST"
        }
        "descNullsFirst" in {
          val q = quote {
            qr1.sortBy(t => t.s)(Ord.descNullsFirst)
          }
          testContext.run(q).string mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t ORDER BY t.s DESC NULLS FIRST"
        }
        "ascNullsLast" in {
          val q = quote {
            qr1.sortBy(t => t.s)(Ord.ascNullsLast)
          }
          testContext.run(q).string mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t ORDER BY t.s ASC NULLS LAST"
        }
        "descNullsLast" in {
          val q = quote {
            qr1.sortBy(t => t.s)(Ord.descNullsLast)
          }
          testContext.run(q).string mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t ORDER BY t.s DESC NULLS LAST"
        }
        "tuple" in {
          val q = quote {
            qr1.sortBy(t => (t.i, t.s))(Ord(Ord.desc, Ord.asc))
          }
          testContext.run(q).string mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t ORDER BY t.i DESC, t.s ASC"
        }
        "expression" in {
          val q = quote {
            qr1.sortBy(t => t.i * 3)
          }
          testContext.run(q).string mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t ORDER BY (t.i * 3) ASC NULLS FIRST"
        }
      }
      "grouped" - {
        "simple" in {
          val q = quote {
            qr1.groupBy(t => t.i).map {
              case (i, entities) => (i, entities.size)
            }
          }
          testContext.run(q).string mustEqual
            "SELECT t._1, t._2 FROM (SELECT t.i _1, COUNT(*) _2 FROM TestEntity t GROUP BY t.i) t"
        }
        "nested" in {
          val q = quote {
            for {
              (a, b) <- qr1.groupBy(t => t.i).map {
                case (i, entities) => (i, entities.size)
              }
              c <- qr2 if (c.i == a)
            } yield {
              (a, b, c)
            }
          }
          testContext.run(q).string mustEqual
            "SELECT t._1, t._2, c.s, c.i, c.l, c.o FROM (SELECT t.i _1, COUNT(*) _2 FROM TestEntity t GROUP BY t.i) t, TestEntity2 c WHERE c.i = t._1"
        }
        "limited" in {
          val q = quote {
            (qr1.groupBy(t => t.i).map {
              case (i, e) =>
                (i, e.map(_.l).min)
            }).take(10)
          }

          testContext.run(q).string mustEqual
            "SELECT t._1, t._2 FROM (SELECT t.i _1, MIN(t.l) _2 FROM TestEntity t GROUP BY t.i) t LIMIT 10"
        }
        "filter.flatMap(groupBy)" in {
          val q = quote {
            for {
              a <- qr1 if a.i == 1
              b <- qr2.groupBy(t => t.i).map { case _ => 1 }
            } yield b
          }
          testContext.run(q).string mustEqual
            "SELECT t.* FROM TestEntity a, (SELECT 1 FROM TestEntity2 t GROUP BY t.i) t WHERE a.i = 1"
        }
      }
      "aggregated" - {
        "min" in {
          val q = quote {
            qr1.map(t => t.i).min
          }
          testContext.run(q).string mustEqual
            "SELECT MIN(t.i) FROM TestEntity t"
        }
        "max" in {
          val q = quote {
            qr1.map(t => t.i).max
          }
          testContext.run(q).string mustEqual
            "SELECT MAX(t.i) FROM TestEntity t"
        }
        "avg" in {
          val q = quote {
            qr1.map(t => t.i).avg
          }
          testContext.run(q).string mustEqual
            "SELECT AVG(t.i) FROM TestEntity t"
        }
        "sum" in {
          val q = quote {
            qr1.map(t => t.i).sum
          }
          testContext.run(q).string mustEqual
            "SELECT SUM(t.i) FROM TestEntity t"
        }
        "size" in {
          val q = quote {
            qr1.size
          }
          testContext.run(q).string mustEqual
            "SELECT COUNT(*) FROM TestEntity x"
        }
        "with filter" in {
          val q = quote {
            qr1.filter(t => t.i > 1).map(t => t.i).min
          }
          testContext.run(q).string mustEqual
            "SELECT MIN(t.i) FROM TestEntity t WHERE t.i > 1"
        }
        "as select value" in {
          val q = quote {
            qr1.take(10).map(a => qr2.filter(t => t.i > a.i).map(t => t.i).min)
          }
          testContext.run(q).string mustEqual
            "SELECT (SELECT MIN(t.i) FROM TestEntity2 t WHERE t.i > a.i) FROM TestEntity a LIMIT 10"
        }
        "after a group by" in {
          val q = quote {
            qr1.groupBy(t => t.s).map { case (a, b) => (a, b.size) }.size
          }
          testContext.run(q).string mustEqual
            "SELECT COUNT(*) FROM (SELECT t.s, COUNT(*) FROM TestEntity t GROUP BY t.s) x"
        }
      }
      "unary operation" - {
        "nonEmpty" in {
          testContext.run(qr1.nonEmpty).string mustEqual
            "SELECT EXISTS (SELECT x.* FROM TestEntity x)"
        }
        "isEmpty" in {
          testContext.run(qr1.isEmpty).string mustEqual
            "SELECT NOT EXISTS (SELECT x.* FROM TestEntity x)"
        }
      }
      "limited" - {
        "simple" in {
          val q = quote {
            qr1.filter(t => t.s != null).take(10)
          }
          testContext.run(q).string mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.s IS NOT NULL LIMIT 10"
        }
        "nested" in {
          val q = quote {
            for {
              a <- qr1.take(1)
              b <- qr2.take(2)
            } yield {
              (a.s, b.i)
            }
          }
          testContext.run(q).string mustEqual
            "SELECT a.s, b.i FROM (SELECT x.s FROM TestEntity x LIMIT 1) a, (SELECT x.i FROM TestEntity2 x LIMIT 2) b"
        }
      }
      "union" - {
        "simple" in {
          val q = quote {
            qr1.filter(t => t.i > 10).union(qr1.filter(t => t.s == "s"))
          }
          testContext.run(q).string mustEqual
            "SELECT x.s, x.i, x.l, x.o FROM ((SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.i > 10) UNION (SELECT t1.s, t1.i, t1.l, t1.o FROM TestEntity t1 WHERE t1.s = 's')) x"
        }
        "mapped" in {
          val q = quote {
            qr1.filter(t => t.i > 10).map(u => u).union(qr1.filter(t => t.s == "s")).map(u => u.s)
          }
          testContext.run(q).string mustEqual
            "SELECT u.s FROM ((SELECT t.s FROM TestEntity t WHERE t.i > 10) UNION (SELECT t1.s FROM TestEntity t1 WHERE t1.s = 's')) u"
        }
        "nested" in {
          val j = quote {
            for {
              a <- qr1
              b <- qr2
            } yield {
              (a, b)
            }
          }
          val q = quote {
            j.union(j).map(u => (u._1.s, u._2.i))
          }
          testContext.run(q.dynamic).string mustEqual
            "SELECT u.s, u.i FROM ((SELECT a.s s, b.i i FROM TestEntity a, TestEntity2 b) UNION (SELECT a1.s s, b1.i i FROM TestEntity a1, TestEntity2 b1)) u"
        }
      }
      "unionAll" - {
        "simple" in {
          val q = quote {
            qr1.filter(t => t.i > 10).unionAll(qr1.filter(t => t.s == "s"))
          }
          testContext.run(q).string mustEqual
            "SELECT x.s, x.i, x.l, x.o FROM ((SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.i > 10) UNION ALL (SELECT t1.s, t1.i, t1.l, t1.o FROM TestEntity t1 WHERE t1.s = 's')) x"
        }
      }
      "join" - {
        "inner" in {
          val q = quote {
            qr1.join(qr2).on((a, b) => a.s == b.s).map(_._1)
          }
          testContext.run(q).string mustEqual
            "SELECT a.s, a.i, a.l, a.o FROM TestEntity a INNER JOIN TestEntity2 b ON a.s = b.s"
        }
        "left" in {
          val q = quote {
            qr1.leftJoin(qr2).on((a, b) => a.s == b.s).map(_._1)
          }
          testContext.run(q).string mustEqual
            "SELECT a.s, a.i, a.l, a.o FROM TestEntity a LEFT JOIN TestEntity2 b ON a.s = b.s"
        }
        "right" in {
          val q = quote {
            qr1.rightJoin(qr2).on((a, b) => a.s == b.s).map(_._2)
          }
          testContext.run(q).string mustEqual
            "SELECT b.s, b.i, b.l, b.o FROM TestEntity a RIGHT JOIN TestEntity2 b ON a.s = b.s"
        }
        "full" in {
          val q = quote {
            qr1.fullJoin(qr2).on((a, b) => a.s == b.s).map(_._1.map(c => c.s))
          }
          testContext.run(q).string mustEqual
            "SELECT a.s FROM TestEntity a FULL JOIN TestEntity2 b ON a.s = b.s"
        }
        "multiple outer joins" in {
          val q = quote {
            qr1.leftJoin(qr2).on((a, b) => a.s == b.s).leftJoin(qr2).on((a, b) => a._1.s == b.s).map(_._1._1)
          }
          testContext.run(q).string mustEqual
            "SELECT a.s, a.i, a.l, a.o FROM TestEntity a LEFT JOIN TestEntity2 b ON a.s = b.s LEFT JOIN TestEntity2 b1 ON a.s = b1.s"
        }
        "with flatMap" - {
          "left" ignore {
            // TODO flatten left flatMaps
            val q = quote {
              qr1.flatMap(a => qr2).leftJoin(qr3).on((b, c) => b.s == c.s).map(_._1)
            }
            testContext.run(q).string mustEqual ""
          }
          "right" in {
            val q = quote {
              qr1.leftJoin(qr2).on((a, b) => a.s == b.s).flatMap(c => qr3)
            }
            testContext.run(q).string mustEqual
              "SELECT x.s, x.i, x.l, x.o FROM TestEntity a LEFT JOIN TestEntity2 b ON a.s = b.s, TestEntity3 x"
          }
        }
      }
      "without from" in {
        val q = quote {
          qr1.map(t => t.i).size == 1L
        }
        testContext.run(q).string mustEqual
          "SELECT (SELECT COUNT(t.i) FROM TestEntity t) = 1"
      }
    }
    "operations" - {
      "unary operation" - {
        "-" in {
          val q = quote {
            qr1.map(t => -t.i)
          }
          testContext.run(q).string mustEqual
            "SELECT - (t.i) FROM TestEntity t"
        }
        "!" in {
          val q = quote {
            qr1.filter(t => !(t.s == "a"))
          }
          testContext.run(q).string mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE NOT (t.s = 'a')"
        }
        "isEmpty" in {
          val q = quote {
            qr1.filter(t => qr2.filter(u => u.s == t.s).isEmpty)
          }
          testContext.run(q).string mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE NOT EXISTS (SELECT u.* FROM TestEntity2 u WHERE u.s = t.s)"
        }
        "nonEmpty" in {
          val q = quote {
            qr1.filter(t => qr2.filter(u => u.s == t.s).nonEmpty)
          }
          testContext.run(q).string mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE EXISTS (SELECT u.* FROM TestEntity2 u WHERE u.s = t.s)"
        }
        "toUpperCase" in {
          val q = quote {
            qr1.map(t => t.s.toUpperCase)
          }
          testContext.run(q).string mustEqual
            "SELECT UPPER (t.s) FROM TestEntity t"
        }
        "toLowerCase" in {
          val q = quote {
            qr1.map(t => t.s.toLowerCase)
          }
          testContext.run(q).string mustEqual
            "SELECT LOWER (t.s) FROM TestEntity t"
        }
        "toLong" in {
          val q = quote {
            qr1.map(t => t.s.toLong)
          }
          testContext.run(q).string mustEqual
            "SELECT  (t.s) FROM TestEntity t"
        }
        "toInt" in {
          val q = quote {
            qr1.map(t => t.s.toInt)
          }
          testContext.run(q).string mustEqual
            "SELECT  (t.s) FROM TestEntity t"
        }
      }
      "binary operation" - {
        "-" in {
          val q = quote {
            qr1.map(t => t.i - t.i)
          }
          testContext.run(q).string mustEqual
            "SELECT t.i - t.i FROM TestEntity t"
        }
        "+" - {
          "numeric" in {
            val q = quote {
              qr1.map(t => t.i + t.i)
            }
            testContext.run(q).string mustEqual
              "SELECT t.i + t.i FROM TestEntity t"
          }
          "string" in {
            val q = quote {
              qr1.map(t => t.s + t.s)
            }
            testContext.run(q).string mustEqual
              "SELECT t.s || t.s FROM TestEntity t"
          }
        }
        "*" in {
          val q = quote {
            qr1.map(t => t.i * t.i)
          }
          testContext.run(q).string mustEqual
            "SELECT t.i * t.i FROM TestEntity t"
        }
        "==" - {
          "null" - {
            "right" in {
              val q = quote {
                qr1.filter(t => t.s == null)
              }
              testContext.run(q).string mustEqual
                "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.s IS NULL"
            }
            "left" in {
              val q = quote {
                qr1.filter(t => null == t.s)
              }
              testContext.run(q).string mustEqual
                "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.s IS NULL"
            }
          }
          "values" in {
            val q = quote {
              qr1.filter(t => t.s == "s")
            }
            testContext.run(q).string mustEqual
              "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.s = 's'"
          }
        }
        "!=" - {
          "null" - {
            "right" in {
              val q = quote {
                qr1.filter(t => t.s != null)
              }
              testContext.run(q).string mustEqual
                "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.s IS NOT NULL"
            }
            "left" in {
              val q = quote {
                qr1.filter(t => null != t.s)
              }
              testContext.run(q).string mustEqual
                "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.s IS NOT NULL"
            }
          }
          "values" in {
            val q = quote {
              qr1.filter(t => t.s != "s")
            }
            testContext.run(q).string mustEqual
              "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.s <> 's'"
          }
        }
        "&&" in {
          val q = quote {
            qr1.filter(t => t.s != null && t.s == "s")
          }
          testContext.run(q).string mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE (t.s IS NOT NULL) AND (t.s = 's')"
        }
        "||" in {
          val q = quote {
            qr1.filter(t => t.s != null || t.s == "s")
          }
          testContext.run(q).string mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE (t.s IS NOT NULL) OR (t.s = 's')"
        }
        ">" in {
          val q = quote {
            qr1.filter(t => t.i > t.l)
          }
          testContext.run(q).string mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.i > t.l"
        }
        ">=" in {
          val q = quote {
            qr1.filter(t => t.i >= t.l)
          }
          testContext.run(q).string mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.i >= t.l"
        }
        "<" in {
          val q = quote {
            qr1.filter(t => t.i < t.l)
          }
          testContext.run(q).string mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.i < t.l"
        }
        "<=" in {
          val q = quote {
            qr1.filter(t => t.i <= t.l)
          }
          testContext.run(q).string mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.i <= t.l"
        }
        "/" in {
          val q = quote {
            qr1.filter(t => (t.i / t.l) == 0)
          }
          testContext.run(q).string mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE (t.i / t.l) = 0"
        }
        "%" in {
          val q = quote {
            qr1.filter(t => (t.i % t.l) == 0)
          }
          testContext.run(q).string mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE (t.i % t.l) = 0"
        }
        "forall" in {
          val q = quote {
            qr1.filter(t => t.i != 1 && t.o.forall(op => op == 1))
          }
          testContext.run(q).string mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE (t.i <> 1) AND ((t.o IS NULL) OR (t.o = 1))"
        }
        "contains" - {
          "query" in {
            val q = quote {
              qr1.filter(t => qr2.map(p => p.i).contains(t.i))
            }
            testContext.run(q).string mustEqual
              "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.i IN (SELECT p.i FROM TestEntity2 p)"
          }
          "option" in {
            val q = quote {
              qr1.filter(t => t.o.contains(1))
            }
            testContext.run(q).string mustEqual
              "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.o = 1"
          }
          "set" - {
            "non-empty" in {
              val q = quote {
                qr1.filter(t => liftQuery(Set(1, 2, 3)).contains(t.i))
              }
              testContext.run(q).string mustEqual "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.i IN (?, ?, ?)"
            }
            "empty" in {
              val q = quote {
                qr1.filter(t => liftQuery(Set.empty[Int]).contains(t.i))
              }
              testContext.run(q).string mustEqual "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE FALSE"
            }
          }

        }
      }
    }
    "action" - {
      "insert" - {
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
        "returning" in {
          val q = quote {
            query[TestEntity].insert(lift(TestEntity("s", 1, 2L, Some(1)))).returning(_.l)
          }
          val run = testContext.run(q).string mustEqual
            "INSERT INTO TestEntity (s,i,o) VALUES (?, ?, ?)"
        }
      }
      "update" - {
        "with filter" in {
          val q = quote {
            qr1.filter(t => t.s == null).update(_.s -> "s")
          }
          testContext.run(q).string mustEqual
            "UPDATE TestEntity SET s = 's' WHERE s IS NULL"
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
      }
      "delete" - {
        "with filter" in {
          val q = quote {
            qr1.filter(t => t.s == null).delete
          }
          testContext.run(q).string mustEqual
            "DELETE FROM TestEntity WHERE s IS NULL"
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
    "ident" in {
      val q = quote {
        qr1.map(t => t.s).filter(s => s == null)
      }
      testContext.run(q).string mustEqual
        "SELECT t.s FROM TestEntity t WHERE t.s IS NULL"
    }
    "value" - {
      "constant" - {
        "string" in {
          val q = quote {
            qr1.map(t => "s")
          }
          testContext.run(q).string mustEqual
            "SELECT 's' FROM TestEntity t"
        }
        "unit" in {
          val q = quote {
            qr1.filter(t => qr1.map(u => {}).isEmpty)
          }
          testContext.run(q).string mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE NOT EXISTS (SELECT 1 FROM TestEntity u)"
        }
        "value" in {
          val q = quote {
            qr1.map(t => 12)
          }
          testContext.run(q).string mustEqual
            "SELECT 12 FROM TestEntity t"
        }
      }
      "null" in {
        val q = quote {
          qr1.update(_.s -> null)
        }
        testContext.run(q).string mustEqual
          "UPDATE TestEntity SET s = null"
      }
      "tuple" in {
        val q = quote {
          qr1.map(t => (1, 2))
        }
        testContext.run(q).string mustEqual
          "SELECT 1, 2 FROM TestEntity t"
      }
    }
    "property" - {
      "column" in {
        val q = quote {
          qr1.map(t => t.s)
        }
        testContext.run(q).string mustEqual
          "SELECT t.s FROM TestEntity t"
      }
      "nested" in {
        case class A(s: String) extends Embedded
        case class B(a: A)
        testContext.run(query[B]).string mustEqual
          "SELECT x.s FROM B x"
      }
      "isEmpty" - {
        "query" in {
          val q = quote {
            qr1.filter(t => t.o.isEmpty)
          }
          testContext.run(q).string mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.o IS NULL"
        }
        "update" in {
          val q = quote {
            qr1.filter(t => t.o.isEmpty).update(_.i -> 1)
          }
          testContext.run(q).string mustEqual
            "UPDATE TestEntity SET i = 1 WHERE o IS NULL"
        }
        "delete" in {
          val q = quote {
            qr1.filter(t => t.o.isEmpty).delete
          }
          testContext.run(q).string mustEqual
            "DELETE FROM TestEntity WHERE o IS NULL"
        }
      }
      "nonEmpty" - {
        "query" in {
          val q = quote {
            qr1.filter(t => t.o.nonEmpty)
          }
          testContext.run(q).string mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.o IS NOT NULL"
        }
        "update" in {
          val q = quote {
            qr1.filter(t => t.o.nonEmpty).update(_.i -> 1)
          }
          testContext.run(q).string mustEqual
            "UPDATE TestEntity SET i = 1 WHERE o IS NOT NULL"
        }
        "delete" in {
          val q = quote {
            qr1.filter(t => t.o.nonEmpty).delete
          }
          testContext.run(q).string mustEqual
            "DELETE FROM TestEntity WHERE o IS NOT NULL"
        }
      }
      "isDefined" - {
        "query" in {
          val q = quote {
            qr1.filter(t => t.o.isDefined)
          }
          testContext.run(q).string mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.o IS NOT NULL"
        }
        "update" in {
          val q = quote {
            qr1.filter(t => t.o.isDefined).update(_.i -> 1)
          }
          testContext.run(q).string mustEqual
            "UPDATE TestEntity SET i = 1 WHERE o IS NOT NULL"
        }
        "delete" in {
          val q = quote {
            qr1.filter(t => t.o.isDefined).delete
          }
          testContext.run(q).string mustEqual
            "DELETE FROM TestEntity WHERE o IS NOT NULL"
        }
      }
    }
    "infix" - {
      "part of the query" in {
        val q = quote {
          qr1.map(t => infix"CONCAT(${t.s}, ${t.s})".as[String])
        }
        testContext.run(q).string mustEqual
          "SELECT CONCAT(t.s, t.s) FROM TestEntity t"
      }
      "source query" in {
        case class Entity(i: Int)
        val q = quote {
          infix"SELECT 1 i FROM DUAL".as[Query[Entity]].map(a => a.i)
        }
        testContext.run(q).string mustEqual
          "SELECT a.i FROM (SELECT 1 i FROM DUAL) a"
      }
      "full infix query" in {
        testContext.run(infix"SELECT * FROM TestEntity".as[Query[TestEntity]]).string mustEqual
          "SELECT x.s, x.i, x.l, x.o FROM (SELECT * FROM TestEntity) x"
      }
      "full infix action" in {
        testContext.run(infix"DELETE FROM TestEntity".as[Action[TestEntity]]).string mustEqual
          "DELETE FROM TestEntity"
      }
    }
    "if" - {
      "simple" in {
        val q = quote {
          qr1.map(t => if (t.i > 0) "a" else "b")
        }
        testContext.run(q).string mustEqual
          "SELECT CASE WHEN t.i > 0 THEN 'a' ELSE 'b' END FROM TestEntity t"
      }
      "nested" in {
        val q = quote {
          qr1.map(t => if (t.i > 0) "a" else if (t.i > 10) "b" else "c")
        }
        testContext.run(q).string mustEqual
          "SELECT CASE WHEN t.i > 0 THEN 'a' WHEN t.i > 10 THEN 'b' ELSE 'c' END FROM TestEntity t"
      }
    }
    "inline vals" - {
      "simple" in {
        val q = quote {
          query[TestEntity].map { x =>
            val (a, b) = (x.i, x.l)
            val ab = a * b
            (a, b, ab)
          }
        }
        testContext.run(q).string mustEqual
          "SELECT x.i, x.l, x.i * x.l FROM TestEntity x"
      }
      "nested" in {
        val q = quote {
          for {
            a <- query[TestEntity]
            b <- query[TestEntity2] if a.i == b.i
            (c, inner) <- {
              val outer = 1
              query[TestEntity3].filter(t => t.i == outer).map { c =>
                val inner = outer + c.i
                (c, inner)
              }
            } if b.i == c.i
          } yield (a, b, c, inner)
        }
        testContext.run(q).string mustEqual
          "SELECT a.s, a.i, a.l, a.o, b.s, b.i, b.l, b.o, t.s, t.i, t.l, t.o, 1 + t.i FROM TestEntity a, TestEntity2 b, TestEntity3 t WHERE (a.i = b.i) AND ((t.i = 1) AND (b.i = t.i))"
      }
      "aggregated" in {
        val q = quote {
          query[TestEntity].map { a =>
            val (b, c) = (query[TestEntity2], query[TestEntity3])
            val (ai, bi, ci) = (a.i, b.map(t => t.i), c.map(t => t.i))
            val (sumB, sumC) = (bi.sum, ci.sum)
            val sumABC = bi.flatMap(b => ci.map(t => ai + b + t)).sum
            (sumB, sumC, sumABC)
          }
        }
        testContext.run(q).string mustEqual
          "SELECT (SELECT SUM(t.i) FROM TestEntity2 t), (SELECT SUM(t1.i) FROM TestEntity3 t1), (SELECT SUM((a.i + t2.i) + t3.i) FROM TestEntity2 t2, TestEntity3 t3) FROM TestEntity a"
      }
    }
  }
}
