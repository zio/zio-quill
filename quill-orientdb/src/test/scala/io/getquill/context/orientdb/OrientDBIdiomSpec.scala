package io.getquill.context.orientdb

import io.getquill.Spec

class OrientDBIdiomSpec extends Spec {

  val ctx = orientdb.mirrorContext
  import ctx._

  "query" - {
    "map" in {
      val q = quote {
        qr1.map(t => t.i)
      }
      ctx.run(q).string mustEqual
        "SELECT i FROM TestEntity"
    }
    "take" in {
      val q = quote {
        qr1.take(1)
      }
      ctx.run(q).string mustEqual
        "SELECT s, i, l, o FROM TestEntity LIMIT 1"
    }
    "sortBy" in {
      val q = quote {
        qr1.sortBy(t => t.i)
      }
      ctx.run(q).string mustEqual
        "SELECT s, i, l, o FROM TestEntity ORDER BY i ASC"
    }
    "allTerms" in {
      val q = quote {
        qr1.filter(t => t.i == 1).sortBy(t => t.s).take(1).map(t => t.s)
      }
      ctx.run(q).string mustEqual
        "SELECT s FROM TestEntity WHERE i = 1 ORDER BY s ASC LIMIT 1"
    }
  }

  "distinct" - {
    "simple" in {
      val q = quote {
        qr1.distinct
      }
      "mirrorContext.run(q).string" mustNot compile
    }
    "distinct single" in {
      val q = quote {
        qr1.map(i => i.i).distinct
      }
      ctx.run(q).string mustEqual
        "SELECT DISTINCT(i) FROM TestEntity"
    }

    "distinct tuple" in {
      val q = quote {
        qr1.map(i => (i.i, i.l)).distinct
      }
      "mirrorContext.run(q).string" mustNot compile
    }
  }

  "order by criteria" - {
    "asc" in {
      val q = quote {
        qr1.sortBy(t => t.i)(Ord.asc)
      }
      ctx.run(q).string mustEqual
        "SELECT s, i, l, o FROM TestEntity ORDER BY i ASC"
    }
    "desc" in {
      val q = quote {
        qr1.sortBy(t => t.i)(Ord.desc)
      }
      ctx.run(q).string mustEqual
        "SELECT s, i, l, o FROM TestEntity ORDER BY i DESC"
    }
    "ascNullsFirst" in {
      val q = quote {
        qr1.sortBy(t => t.i)(Ord.ascNullsFirst)
      }
      ctx.run(q).string mustEqual
        "SELECT s, i, l, o FROM TestEntity ORDER BY i ASC"
    }
    "descNullsFirst" in {
      val q = quote {
        qr1.sortBy(t => t.i)(Ord.descNullsFirst)
      }
      ctx.run(q).string mustEqual
        "SELECT s, i, l, o FROM TestEntity ORDER BY i DESC"
    }
    "ascNullsLast" in {
      val q = quote {
        qr1.sortBy(t => t.i)(Ord.ascNullsLast)
      }
      ctx.run(q).string mustEqual
        "SELECT s, i, l, o FROM TestEntity ORDER BY i ASC"
    }
    "descNullsLast" in {
      val q = quote {
        qr1.sortBy(t => t.i)(Ord.descNullsLast)
      }
      ctx.run(q).string mustEqual
        "SELECT s, i, l, o FROM TestEntity ORDER BY i DESC"
    }
  }

  "operation" - {
    "binary" in {
      val q = quote {
        qr1.filter(t => t.i == 1)
      }
      ctx.run(q).string mustEqual
        "SELECT s, i, l, o FROM TestEntity WHERE i = 1"
    }
    "unary (not supported)" in {
      val q = quote {
        qr1.filter(t => !(t.i == 1))
      }
      "mirrorContext.run(q)" mustNot compile
    }
    "function apply (not supported)" in {
      val q = quote {
        qr1.filter(t => infix"f".as[Int => Boolean](t.i))
      }
      "mirrorContext.run(q)" mustNot compile
    }
  }

  "aggregation" - {
    "count" in {
      val q = quote {
        qr1.filter(t => t.i == 1).size
      }
      ctx.run(q).string mustEqual
        "SELECT COUNT(*) FROM TestEntity WHERE i = 1"
    }
    "max" in {
      val q = quote {
        qr1.map(t => t.i).max
      }
      ctx.run(q).string mustEqual
        "SELECT MAX(i) FROM TestEntity"
    }
  }

  "binary operation" - {
    "==" in {
      val q = quote {
        qr1.filter(t => t.i == 1)
      }
      ctx.run(q).string mustEqual
        "SELECT s, i, l, o FROM TestEntity WHERE i = 1"
    }
    "&&" in {
      val q = quote {
        qr1.filter(t => t.i == 1 && t.s == "s")
      }
      ctx.run(q).string mustEqual
        "SELECT s, i, l, o FROM TestEntity WHERE (i = 1) AND (s = 's')"
    }
    ">" in {
      val q = quote {
        qr1.filter(t => t.i > 1)
      }
      ctx.run(q).string mustEqual
        "SELECT s, i, l, o FROM TestEntity WHERE i > 1"
    }
    ">=" in {
      val q = quote {
        qr1.filter(t => t.i >= 1)
      }
      ctx.run(q).string mustEqual
        "SELECT s, i, l, o FROM TestEntity WHERE i >= 1"
    }
    "<" in {
      val q = quote {
        qr1.filter(t => t.i < 1)
      }
      ctx.run(q).string mustEqual
        "SELECT s, i, l, o FROM TestEntity WHERE i < 1"
    }
    "<=" in {
      val q = quote {
        qr1.filter(t => t.i <= 1)
      }
      ctx.run(q).string mustEqual
        "SELECT s, i, l, o FROM TestEntity WHERE i <= 1"
    }
    "+" in {
      val q = quote {
        qr1.update(t => t.i -> 1)
      }
      ctx.run(q).string mustEqual
        "UPDATE TestEntity SET i = 1"
    }
    "*" in {
      val q = quote {
        qr1.filter(t => t.i * 2 == 4)
      }
      ctx.run(q).string mustEqual
        "SELECT s, i, l, o FROM TestEntity WHERE (i * 2) = 4"
    }
    "-" in {
      val q = quote {
        qr1.filter(t => t.i - 1 == 1)
      }
      ctx.run(q).string mustEqual
        "SELECT s, i, l, o FROM TestEntity WHERE (i - 1) = 1"
    }
    "/" in {
      val q = quote {
        qr1.filter(t => t.i / 1 == 1)
      }
      ctx.run(q).string mustEqual
        "SELECT s, i, l, o FROM TestEntity WHERE (i / 1) = 1"
    }
    "%" in {
      val q = quote {
        qr1.filter(t => t.i % 1 == 0)
      }
      ctx.run(q).string mustEqual
        "SELECT s, i, l, o FROM TestEntity WHERE (i % 1) = 0"
    }
  }
  "value" - {
    "string" in {
      val q = quote {
        qr1.filter(t => t.s == "a")
      }
      ctx.run(q).string mustEqual
        "SELECT s, i, l, o FROM TestEntity WHERE s = 'a'"
    }
    "unit" in {
      case class Test(u: Unit)
      val q = quote {
        query[Test].filter(t => t.u == (())).size
      }
      ctx.run(q).string mustEqual
        "SELECT COUNT(*) FROM Test WHERE u = 1"
    }
    "int" in {
      val q = quote {
        qr1.filter(t => t.i == 1)
      }
      ctx.run(q).string mustEqual
        "SELECT s, i, l, o FROM TestEntity WHERE i = 1"
    }
    "tuple" in {
      val q = quote {
        qr1.map(t => (t.i, t.s))
      }
      ctx.run(q).string mustEqual
        "SELECT i, s FROM TestEntity"
    }
    "caseclass" in {
      case class IntString(intProp: Int, stringProp: String)
      val q = quote {
        qr1.map(t => new IntString(t.i, t.s))
      }
      ctx.run(q).string mustEqual
        "SELECT i, s FROM TestEntity"
    }
    "null" in {
      val q = quote {
        qr1.filter(t => t.s == null)
      }
      ctx.run(q).string mustEqual
        "SELECT s, i, l, o FROM TestEntity WHERE s IS NULL"
    }
  }

  "action" - {
    "insert" in {
      val q = quote {
        qr1.insert(lift(TestEntity("a", 1, 1L, None)))
      }
      ctx.run(q).string mustEqual
        "INSERT INTO TestEntity (s, i, l, o) VALUES(?, ?, ?, ?)"
    }
    "update" - {
      "all" in {
        val q = quote {
          qr1.update(lift(TestEntity("a", 1, 1L, None)))
        }
        ctx.run(q).string mustEqual
          "UPDATE TestEntity SET s = ?, i = ?, l = ?, o = ?"
      }
      "filtered" in {
        val q = quote {
          qr1.filter(t => t.i == 1).update(lift(TestEntity("a", 1, 1L, None)))
        }
        ctx.run(q).string mustEqual
          "UPDATE TestEntity SET s = ?, i = ?, l = ?, o = ? WHERE i = 1"
      }
    }
    "delete" - {
      "filtered" in {
        val q = quote {
          qr1.filter(t => t.i == 1).delete
        }
        ctx.run(q).string mustEqual
          "DELETE FROM TestEntity WHERE i = 1"
      }
      "all" in {
        val q = quote {
          qr1.delete
        }
        ctx.run(q).string mustEqual
          "DELETE FROM TestEntity"
      }
    }
  }

  "infix" - {
    "query" - {
      "partial" in {
        val q = quote {
          qr1.filter(t => infix"${t.i} = 1".as[Boolean])
        }
        ctx.run(q).string mustEqual
          "SELECT s, i, l, o FROM TestEntity WHERE i = 1"
      }
      "full" in {
        val q = quote {
          infix"SELECT MODE(i) FROM TestEntity".as[Query[Int]]
        }
        ctx.run(q).string mustEqual
          "SELECT * FROM (SELECT MODE(i) FROM TestEntity)"
      }
    }
    "action" - {
      "partial" in {
        val q = quote {
          qr1.filter(t => infix"${t.i} = 1".as[Boolean]).update(lift(TestEntity("a", 1, 1L, None)))
        }
        ctx.run(q).string mustEqual
          "UPDATE TestEntity SET s = ?, i = ?, l = ?, o = ? WHERE i = 1"
      }
      "full" in {
        val q = quote {
          infix"DELETE FROM TestEntity".as[Action[Int]]
        }
        ctx.run(q).string mustEqual
          "DELETE FROM TestEntity"
      }
    }
  }
}