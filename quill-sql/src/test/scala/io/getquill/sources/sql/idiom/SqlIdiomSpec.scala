package io.getquill.sources.sql.idiom

import io.getquill._
import io.getquill.Spec
import io.getquill.naming.{ SnakeCase, UpperCase, Escape, Literal }
import io.getquill.sources.sql.mirrorSource
import io.getquill.ast.Ast

class SqlIdiomSpec extends Spec {

  "shows the sql representation of normalized asts" - {
    "query" - {
      "without filter" in {
        mirrorSource.run(qr1).sql mustEqual
          "SELECT x.s, x.i, x.l, x.o FROM TestEntity x"
      }
      "with filter" in {
        val q = quote {
          qr1.filter(t => t.s == "s")
        }
        mirrorSource.run(q).sql mustEqual
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
        mirrorSource.run(q).sql mustEqual
          "SELECT a.s FROM TestEntity a, TestEntity2 b WHERE a.s = b.s"
      }
      "sorted" - {
        "simple" in {
          val q = quote {
            qr1.filter(t => t.s != null).sortBy(_.s)
          }
          mirrorSource.run(q).sql mustEqual
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
          mirrorSource.run(q).sql mustEqual
            "SELECT t.s, t1.i FROM (SELECT t.s FROM TestEntity t ORDER BY t.s ASC NULLS FIRST) t, (SELECT t1.i FROM TestEntity2 t1 ORDER BY t1.i ASC NULLS FIRST) t1"
        }
        "asc" in {
          val q = quote {
            qr1.sortBy(t => t.s)(Ord.asc)
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t ORDER BY t.s ASC"
        }
        "desc" in {
          val q = quote {
            qr1.sortBy(t => t.s)(Ord.desc)
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t ORDER BY t.s DESC"
        }
        "ascNullsFirst" in {
          val q = quote {
            qr1.sortBy(t => t.s)(Ord.ascNullsFirst)
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t ORDER BY t.s ASC NULLS FIRST"
        }
        "descNullsFirst" in {
          val q = quote {
            qr1.sortBy(t => t.s)(Ord.descNullsFirst)
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t ORDER BY t.s DESC NULLS FIRST"
        }
        "ascNullsLast" in {
          val q = quote {
            qr1.sortBy(t => t.s)(Ord.ascNullsLast)
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t ORDER BY t.s ASC NULLS LAST"
        }
        "descNullsLast" in {
          val q = quote {
            qr1.sortBy(t => t.s)(Ord.descNullsLast)
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t ORDER BY t.s DESC NULLS LAST"
        }
        "tuple" in {
          val q = quote {
            qr1.sortBy(t => (t.i, t.s))(Ord(Ord.desc, Ord.asc))
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t ORDER BY t.i DESC, t.s ASC"
        }
        "expression" in {
          val q = quote {
            qr1.sortBy(t => t.i * 3)
          }
          mirrorSource.run(q).sql mustEqual
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
          mirrorSource.run(q).sql mustEqual
            "SELECT t.i, COUNT(*) FROM TestEntity t GROUP BY t.i"
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
          mirrorSource.run(q).sql mustEqual
            "SELECT t._1, t._2, c.s, c.i, c.l, c.o FROM (SELECT t.i _1, COUNT(*) _2 FROM TestEntity t GROUP BY t.i) t, TestEntity2 c WHERE c.i = t._1"
        }
        "limited" in {
          val q = quote {
            (qr1.groupBy(t => t.i).map {
              case (i, e) =>
                (i, e.map(_.l).min)
            }).take(10)
          }

          mirrorSource.run(q).sql mustEqual
            "SELECT t._1, t._2 FROM (SELECT t.i _1, MIN(t.l) _2 FROM TestEntity t GROUP BY t.i) t LIMIT 10"
        }
      }
      "aggregated" - {
        "min" in {
          val q = quote {
            qr1.map(t => t.i).min
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT MIN(t.i) FROM TestEntity t"
        }
        "max" in {
          val q = quote {
            qr1.map(t => t.i).max
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT MAX(t.i) FROM TestEntity t"
        }
        "avg" in {
          val q = quote {
            qr1.map(t => t.i).avg
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT AVG(t.i) FROM TestEntity t"
        }
        "sum" in {
          val q = quote {
            qr1.map(t => t.i).sum
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT SUM(t.i) FROM TestEntity t"
        }
        "size" in {
          val q = quote {
            qr1.size
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT COUNT(*) FROM TestEntity x"
        }
        "with filter" in {
          val q = quote {
            qr1.filter(t => t.i > 1).map(t => t.i).min
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT MIN(t.i) FROM TestEntity t WHERE t.i > 1"
        }
        "as select value" in {
          val q = quote {
            qr1.take(10).map(a => qr2.filter(t => t.i > a.i).map(t => t.i).min)
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT (SELECT MIN(t.i) FROM TestEntity2 t WHERE t.i > a.i) FROM TestEntity a LIMIT 10"
        }
        "after a group by" in {
          val q = quote {
            qr1.groupBy(t => t.s).map { case (a, b) => (a, b.size) }.size
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT COUNT(*) FROM (SELECT t.s, COUNT(*) FROM TestEntity t GROUP BY t.s) x"
        }
      }
      "limited" - {
        "simple" in {
          val q = quote {
            qr1.filter(t => t.s != null).take(10)
          }
          mirrorSource.run(q).sql mustEqual
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
          mirrorSource.run(q).sql mustEqual
            "SELECT a.s, b.i FROM (SELECT x.s FROM TestEntity x LIMIT 1) a, (SELECT x.i FROM TestEntity2 x LIMIT 2) b"
        }
      }
      "union" - {
        "simple" in {
          val q = quote {
            qr1.filter(t => t.i > 10).union(qr1.filter(t => t.s == "s"))
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT x.s, x.i, x.l, x.o FROM (SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.i > 10 UNION SELECT t1.s, t1.i, t1.l, t1.o FROM TestEntity t1 WHERE t1.s = 's') x"
        }
        "mapped" in {
          val q = quote {
            qr1.filter(t => t.i > 10).map(u => u).union(qr1.filter(t => t.s == "s")).map(u => u.s)
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT u.s FROM (SELECT t.s FROM TestEntity t WHERE t.i > 10 UNION SELECT t1.s FROM TestEntity t1 WHERE t1.s = 's') u"
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
          mirrorSource.run(q).sql mustEqual
            "SELECT u._1s, u._2i FROM (SELECT a.s _1s, b.i _2i FROM TestEntity a, TestEntity2 b UNION SELECT a1.s _1s, b1.i _2i FROM TestEntity a1, TestEntity2 b1) u"
        }
      }
      "unionAll" - {
        "simple" in {
          val q = quote {
            qr1.filter(t => t.i > 10).unionAll(qr1.filter(t => t.s == "s"))
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT x.s, x.i, x.l, x.o FROM (SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.i > 10 UNION ALL SELECT t1.s, t1.i, t1.l, t1.o FROM TestEntity t1 WHERE t1.s = 's') x"
        }
      }
      "join" - {
        "inner" in {
          val q = quote {
            qr1.join(qr2).on((a, b) => a.s == b.s).map(_._1)
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT a.s, a.i, a.l, a.o FROM TestEntity a INNER JOIN TestEntity2 b ON a.s = b.s"
        }
        "left" in {
          val q = quote {
            qr1.leftJoin(qr2).on((a, b) => a.s == b.s).map(_._1)
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT a.s, a.i, a.l, a.o FROM TestEntity a LEFT JOIN TestEntity2 b ON a.s = b.s"
        }
        "right" in {
          val q = quote {
            qr1.rightJoin(qr2).on((a, b) => a.s == b.s).map(_._2)
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT b.s, b.i, b.l, b.o FROM TestEntity a RIGHT JOIN TestEntity2 b ON a.s = b.s"
        }
        "full" in {
          val q = quote {
            qr1.fullJoin(qr2).on((a, b) => a.s == b.s).map(_._1.map(c => c.s))
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT a.s FROM TestEntity a FULL JOIN TestEntity2 b ON a.s = b.s"
        }
        "multiple outer joins" in {
          val q = quote {
            qr1.leftJoin(qr2).on((a, b) => a.s == b.s).leftJoin(qr2).on((a, b) => a._1.s == b.s).map(_._1._1)
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT a.s, a.i, a.l, a.o FROM TestEntity a LEFT JOIN TestEntity2 b ON a.s = b.s LEFT JOIN TestEntity2 b ON a.s = b.s"
        }
        "with flatMap" - {
          "left" ignore {
            // TODO flatten left flatMaps
            val q = quote {
              qr1.flatMap(a => qr2).leftJoin(qr3).on((b, c) => b.s == c.s).map(_._1)
            }
            mirrorSource.run(q).sql mustEqual ""
          }
          "right" in {
            val q = quote {
              qr1.leftJoin(qr2).on((a, b) => a.s == b.s).flatMap(c => qr3)
            }
            mirrorSource.run(q).sql mustEqual
              "SELECT x.s, x.i, x.l, x.o FROM TestEntity a LEFT JOIN TestEntity2 b ON a.s = b.s, TestEntity3 x"
          }
        }
      }
    }
    "operations" - {
      "unary operation" - {
        "-" in {
          val q = quote {
            qr1.map(t => -t.i)
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT - (t.i) FROM TestEntity t"
        }
        "!" in {
          val q = quote {
            qr1.filter(t => !(t.s == "a"))
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE NOT (t.s = 'a')"
        }
        "isEmpty" in {
          val q = quote {
            qr1.filter(t => qr2.filter(u => u.s == t.s).isEmpty)
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE NOT EXISTS (SELECT u.* FROM TestEntity2 u WHERE u.s = t.s)"
        }
        "nonEmpty" in {
          val q = quote {
            qr1.filter(t => qr2.filter(u => u.s == t.s).nonEmpty)
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE EXISTS (SELECT u.* FROM TestEntity2 u WHERE u.s = t.s)"
        }
        "toUpperCase" in {
          val q = quote {
            qr1.map(t => t.s.toUpperCase)
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT UPPER (t.s) FROM TestEntity t"
        }
        "toLowerCase" in {
          val q = quote {
            qr1.map(t => t.s.toLowerCase)
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT LOWER (t.s) FROM TestEntity t"
        }
      }
      "binary operation" - {
        "-" in {
          val q = quote {
            qr1.map(t => t.i - t.i)
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT t.i - t.i FROM TestEntity t"
        }
        "+" - {
          "numeric" in {
            val q = quote {
              qr1.map(t => t.i + t.i)
            }
            mirrorSource.run(q).sql mustEqual
              "SELECT t.i + t.i FROM TestEntity t"
          }
          "string" in {
            val q = quote {
              qr1.map(t => t.s + t.s)
            }
            mirrorSource.run(q).sql mustEqual
              "SELECT t.s || t.s FROM TestEntity t"
          }
        }
        "*" in {
          val q = quote {
            qr1.map(t => t.i * t.i)
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT t.i * t.i FROM TestEntity t"
        }
        "==" - {
          "null" - {
            "right" in {
              val q = quote {
                qr1.filter(t => t.s == null)
              }
              mirrorSource.run(q).sql mustEqual
                "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.s IS NULL"
            }
            "left" in {
              val q = quote {
                qr1.filter(t => null == t.s)
              }
              mirrorSource.run(q).sql mustEqual
                "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.s IS NULL"
            }
          }
          "values" in {
            val q = quote {
              qr1.filter(t => t.s == "s")
            }
            mirrorSource.run(q).sql mustEqual
              "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.s = 's'"
          }
        }
        "!=" - {
          "null" - {
            "right" in {
              val q = quote {
                qr1.filter(t => t.s != null)
              }
              mirrorSource.run(q).sql mustEqual
                "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.s IS NOT NULL"
            }
            "left" in {
              val q = quote {
                qr1.filter(t => null != t.s)
              }
              mirrorSource.run(q).sql mustEqual
                "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.s IS NOT NULL"
            }
          }
          "values" in {
            val q = quote {
              qr1.filter(t => t.s != "s")
            }
            mirrorSource.run(q).sql mustEqual
              "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.s <> 's'"
          }
        }
        "&&" in {
          val q = quote {
            qr1.filter(t => t.i != null && t.s == "s")
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE (t.i IS NOT NULL) AND (t.s = 's')"
        }
        "||" in {
          val q = quote {
            qr1.filter(t => t.i != null || t.s == "s")
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE (t.i IS NOT NULL) OR (t.s = 's')"
        }
        ">" in {
          val q = quote {
            qr1.filter(t => t.i > t.l)
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.i > t.l"
        }
        ">=" in {
          val q = quote {
            qr1.filter(t => t.i >= t.l)
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.i >= t.l"
        }
        "<" in {
          val q = quote {
            qr1.filter(t => t.i < t.l)
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.i < t.l"
        }
        "<=" in {
          val q = quote {
            qr1.filter(t => t.i <= t.l)
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.i <= t.l"
        }
        "/" in {
          val q = quote {
            qr1.filter(t => (t.i / t.l) == 0)
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE (t.i / t.l) = 0"
        }
        "%" in {
          val q = quote {
            qr1.filter(t => (t.i % t.l) == 0)
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE (t.i % t.l) = 0"
        }
      }
      "function apply" in {
        import io.getquill.util.Show._
        implicit val naming = new Literal {}
        import FallbackDialect._
        val q = quote {
          ((i: Int) => i + 1).apply(2)
        }
        val e = intercept[IllegalStateException] {
          (q.ast: Ast).show
        }
      }
    }
    "action" - {
      "insert" - {
        "simple" in {
          val q = quote {
            qr1.insert(_.i -> 1, _.s -> "s")
          }
          mirrorSource.run(q).sql mustEqual
            "INSERT INTO TestEntity (i,s) VALUES (1, 's')"
        }
        "using nested select" in {
          val q = quote {
            qr1.insert(_.i -> qr2.map(t => t.i).max, _.s -> "s")
          }
          mirrorSource.run(q).sql mustEqual
            "INSERT INTO TestEntity (i,s) VALUES ((SELECT MAX(t.i) FROM TestEntity2 t), 's')"
        }
      }
      "update" - {
        "with filter" in {
          val q = quote {
            qr1.filter(t => t.s == null).update(_.s -> "s")
          }
          mirrorSource.run(q).sql mustEqual
            "UPDATE TestEntity SET s = 's' WHERE s IS NULL"
        }
        "without filter" in {
          val q = quote {
            qr1.update(_.s -> "s")
          }
          mirrorSource.run(q).sql mustEqual
            "UPDATE TestEntity SET s = 's'"
        }
        "using a table column" in {
          val q = quote {
            qr1.update(t => t.i -> (t.i + 1))
          }
          mirrorSource.run(q).sql mustEqual
            "UPDATE TestEntity SET i = (i + 1)"
        }
        "using nested select" in {
          val q = quote {
            qr1.update(_.i -> qr2.map(t => t.i).max)
          }
          mirrorSource.run(q).sql mustEqual
            "UPDATE TestEntity SET i = (SELECT MAX(t.i) FROM TestEntity2 t)"
        }
      }
      "delete" - {
        "with filter" in {
          val q = quote {
            qr1.filter(t => t.s == null).delete
          }
          mirrorSource.run(q).sql mustEqual
            "DELETE FROM TestEntity WHERE s IS NULL"
        }
        "without filter" in {
          val q = quote {
            qr1.delete
          }
          mirrorSource.run(q).sql mustEqual
            "DELETE FROM TestEntity"
        }
      }
    }
    "ident" in {
      val q = quote {
        qr1.map(t => t.s).filter(s => s == null)
      }
      mirrorSource.run(q).sql mustEqual
        "SELECT t.s FROM TestEntity t WHERE t.s IS NULL"
    }
    "value" - {
      "constant" - {
        "string" in {
          val q = quote {
            qr1.map(t => "s")
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT 's' FROM TestEntity t"
        }
        "unit" in {
          val q = quote {
            qr1.filter(t => qr1.map(u => {}).isEmpty)
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE NOT EXISTS (SELECT 1 FROM TestEntity u)"
        }
        "value" in {
          val q = quote {
            qr1.map(t => 12)
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT 12 FROM TestEntity t"
        }
      }
      "null" in {
        val q = quote {
          qr1.update(_.s -> null)
        }
        mirrorSource.run(q).sql mustEqual
          "UPDATE TestEntity SET s = null"
      }
      "tuple" in {
        val q = quote {
          qr1.filter(t => (1, 2) == t.i)
        }
        mirrorSource.run(q).sql mustEqual
          "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE (1, 2) = t.i"
      }
    }
    "property" in {
      val q = quote {
        qr1.map(t => t.s)
      }
      mirrorSource.run(q).sql mustEqual
        "SELECT t.s FROM TestEntity t"
    }
    "infix" - {
      "part of the query" in {
        val q = quote {
          qr1.map(t => infix"CONCAT(${t.s}, ${t.s})".as[String])
        }
        mirrorSource.run(q).sql mustEqual
          "SELECT CONCAT(t.s, t.s) FROM TestEntity t"
      }
      "source query" in {
        case class Entity(i: Int)
        val q = quote {
          infix"SELECT 1 i FROM DUAL".as[Query[Entity]].map(a => a.i)
        }
        mirrorSource.run(q).sql mustEqual
          "SELECT a.i FROM (SELECT 1 i FROM DUAL) a"
      }
      "full infix query" in {
        mirrorSource.run(infix"SELECT * FROM TestEntity".as[Query[TestEntity]]).sql mustEqual
          "SELECT x.s, x.i, x.l, x.o FROM (SELECT * FROM TestEntity) x"
      }
      "full infix action" in {
        mirrorSource.run(infix"DELETE FROM TestEntity".as[Action[TestEntity]]).sql mustEqual
          "DELETE FROM TestEntity"
      }
    }
    "if" - {
      "simple" in {
        val q = quote {
          qr1.map(t => if (t.i > 0) "a" else "b")
        }
        mirrorSource.run(q).sql mustEqual
          "SELECT CASE WHEN t.i > 0 THEN 'a' ELSE 'b' END FROM TestEntity t"
      }
      "nested" in {
        val q = quote {
          qr1.map(t => if (t.i > 0) "a" else if (t.i > 10) "b" else "c")
        }
        mirrorSource.run(q).sql mustEqual
          "SELECT CASE WHEN t.i > 0 THEN 'a' WHEN t.i > 10 THEN 'b' ELSE 'c' END FROM TestEntity t"
      }
    }
  }

  "fails if the query is malformed" in {
    val q = quote {
      qr1.filter(t => t == ((s: String) => s))
    }
    "mirrorSource.run(q)" mustNot compile
  }
}
