package io.getquill.source.sql.idiom

import io.getquill._
import io.getquill.Spec
import io.getquill.source.sql.mirror.mirrorSource.run
import io.getquill.source.sql.mirror.mirrorSource

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
            "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE t.s IS NOT NULL ORDER BY t.s"
        }
        "nested" in {
          val q = quote {
            for {
              a <- qr1.sortBy(t => t.s).reverse
              b <- qr2.sortBy(t => t.i)
            } yield {
              (a.s, b.i)
            }
          }
          mirrorSource.run(q).sql mustEqual
            "SELECT t.s, t1.i FROM (SELECT * FROM TestEntity t ORDER BY t.s DESC) t, TestEntity2 t1 ORDER BY t1.i"
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
            "SELECT a.s, b.i FROM (SELECT * FROM TestEntity x LIMIT 1) a, TestEntity2 b LIMIT 2"
        }
      }
    }
    "unary operation" - {
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
          "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE NOT EXISTS (SELECT * FROM TestEntity2 u WHERE u.s = t.s)"
      }
      "nonEmpty" in {
        val q = quote {
          qr1.filter(t => qr2.filter(u => u.s == t.s).nonEmpty)
        }
        mirrorSource.run(q).sql mustEqual
          "SELECT t.s, t.i, t.l, t.o FROM TestEntity t WHERE EXISTS (SELECT * FROM TestEntity2 u WHERE u.s = t.s)"
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
      "+" in {
        val q = quote {
          qr1.map(t => t.i + t.i)
        }
        mirrorSource.run(q).sql mustEqual
          "SELECT t.i + t.i FROM TestEntity t"
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
    "action" - {
      "insert" in {
        val q = quote {
          qr1.insert(_.i -> 1, _.s -> "s")
        }
        mirrorSource.run(q).sql mustEqual
          "INSERT INTO TestEntity (i,s) VALUES (1, 's')"
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
          qr1.map(t => (t.s, t.i))
        }
        mirrorSource.run(q).sql mustEqual
          "SELECT t.s, t.i FROM TestEntity t"
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
          infix"SELECT 1 i FROM DUAL".as[Queryable[Entity]].map(a => a.i)
        }
        mirrorSource.run(q).sql mustEqual
          "SELECT a.i FROM (SELECT 1 i FROM DUAL) a"
      }
      "full infix query" in {
        mirrorSource.run(infix"SELECT * FROM TestEntity".as[Queryable[TestEntity]]).sql mustEqual
          "SELECT x.s, x.i, x.l, x.o FROM (SELECT * FROM TestEntity) x"
      }
      "full infix action" in {
        mirrorSource.run(infix"DELETE FROM TestEntity".as[Actionable[TestEntity]]).sql mustEqual
          "DELETE FROM TestEntity"
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
