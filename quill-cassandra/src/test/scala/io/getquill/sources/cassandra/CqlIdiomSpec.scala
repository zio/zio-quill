package io.getquill.sources.cassandra

import io.getquill._
import io.getquill.naming.Literal

class CqlIdiomSpec extends Spec {

  "query" - {
    "map" in {
      val q = quote {
        qr1.map(t => t.i)
      }
      mirrorSource.run(q).cql mustEqual
        "SELECT i FROM TestEntity"
    }
    "take" in {
      val q = quote {
        qr1.take(1)
      }
      mirrorSource.run(q).cql mustEqual
        "SELECT s, i, l, o FROM TestEntity LIMIT 1"
    }
    "sortBy" in {
      val q = quote {
        qr1.sortBy(t => t.i)
      }
      mirrorSource.run(q).cql mustEqual
        "SELECT s, i, l, o FROM TestEntity ORDER BY i ASC"
    }
    "all terms" in {
      val q = quote {
        qr1.filter(t => t.i == 1).sortBy(t => t.s).take(1).map(t => t.s)
      }
      mirrorSource.run(q).cql mustEqual
        "SELECT s FROM TestEntity WHERE i = 1 ORDER BY s ASC LIMIT 1"
    }
  }

  "order by criteria" - {
    "asc" in {
      val q = quote {
        qr1.sortBy(t => t.i)(Ord.asc)
      }
      mirrorSource.run(q).cql mustEqual
        "SELECT s, i, l, o FROM TestEntity ORDER BY i ASC"
    }
    "desc" in {
      val q = quote {
        qr1.sortBy(t => t.i)(Ord.desc)
      }
      mirrorSource.run(q).cql mustEqual
        "SELECT s, i, l, o FROM TestEntity ORDER BY i DESC"
    }
    "ascNullsFirst" in {
      val q = quote {
        qr1.sortBy(t => t.i)(Ord.ascNullsFirst)
      }
      mirrorSource.run(q).cql mustEqual
        "SELECT s, i, l, o FROM TestEntity ORDER BY i ASC"
    }
    "descNullsFirst" in {
      val q = quote {
        qr1.sortBy(t => t.i)(Ord.descNullsFirst)
      }
      mirrorSource.run(q).cql mustEqual
        "SELECT s, i, l, o FROM TestEntity ORDER BY i DESC"
    }
    "ascNullsLast" in {
      val q = quote {
        qr1.sortBy(t => t.i)(Ord.ascNullsLast)
      }
      mirrorSource.run(q).cql mustEqual
        "SELECT s, i, l, o FROM TestEntity ORDER BY i ASC"
    }
    "descNullsLast" in {
      val q = quote {
        qr1.sortBy(t => t.i)(Ord.descNullsLast)
      }
      mirrorSource.run(q).cql mustEqual
        "SELECT s, i, l, o FROM TestEntity ORDER BY i DESC"
    }
  }

  "operation" - {
    "binary" in {
      val q = quote {
        qr1.filter(t => t.i == 1)
      }
      mirrorSource.run(q).cql mustEqual
        "SELECT s, i, l, o FROM TestEntity WHERE i = 1"
    }
    "unary (not supported)" in {
      val q = quote {
        qr1.filter(t => !(t.i == 1))
      }
      "mirrorSource.run(q)" mustNot compile
    }
    "function apply (not supported)" in {
      val q = quote {
        qr1.filter(t => infix"f".as[Int => Boolean](t.i))
      }
      "mirrorSource.run(q)" mustNot compile
    }
  }

  "aggregation" - {
    "count" in {
      val q = quote {
        qr1.filter(t => t.i == 1).size
      }
      mirrorSource.run(q).cql mustEqual
        "SELECT COUNT(1) FROM TestEntity WHERE i = 1"
    }
    "invalid" in {
      val q = quote {
        qr1.map(t => t.i).max
      }
      "mirrorSource.run(q)" mustNot compile
    }
  }

  "binary operation" - {
    "==" in {
      val q = quote {
        qr1.filter(t => t.i == 1)
      }
      mirrorSource.run(q).cql mustEqual
        "SELECT s, i, l, o FROM TestEntity WHERE i = 1"
    }
    "&&" in {
      val q = quote {
        qr1.filter(t => t.i == 1 && t.s == "s")
      }
      mirrorSource.run(q).cql mustEqual
        "SELECT s, i, l, o FROM TestEntity WHERE i = 1 AND s = 's'"
    }
    ">" in {
      val q = quote {
        qr1.filter(t => t.i > 1)
      }
      mirrorSource.run(q).cql mustEqual
        "SELECT s, i, l, o FROM TestEntity WHERE i > 1"
    }
    ">=" in {
      val q = quote {
        qr1.filter(t => t.i >= 1)
      }
      mirrorSource.run(q).cql mustEqual
        "SELECT s, i, l, o FROM TestEntity WHERE i >= 1"
    }
    "<" in {
      val q = quote {
        qr1.filter(t => t.i < 1)
      }
      mirrorSource.run(q).cql mustEqual
        "SELECT s, i, l, o FROM TestEntity WHERE i < 1"
    }
    "<=" in {
      val q = quote {
        qr1.filter(t => t.i <= 1)
      }
      mirrorSource.run(q).cql mustEqual
        "SELECT s, i, l, o FROM TestEntity WHERE i <= 1"
    }
    "invalid" in {
      val q = quote {
        qr1.filter(t => t.i * 2 == 4)
      }
      "mirrorSource.run(q)" mustNot compile
    }
  }

  "value" - {
    "string" in {
      val q = quote {
        qr1.filter(t => t.s == "s")
      }
      mirrorSource.run(q).cql mustEqual
        "SELECT s, i, l, o FROM TestEntity WHERE s = 's'"
    }
    "unit" in {
      val q = quote {
        qr1.filter(t => t.i == (()))
      }
      mirrorSource.run(q).cql mustEqual
        "SELECT s, i, l, o FROM TestEntity WHERE i = 1"
    }
    "int" in {
      val q = quote {
        qr1.filter(t => t.i == 1)
      }
      mirrorSource.run(q).cql mustEqual
        "SELECT s, i, l, o FROM TestEntity WHERE i = 1"
    }
    "tuple" in {
      val q = quote {
        qr1.map(t => (t.i, t.s))
      }
      mirrorSource.run(q).cql mustEqual
        "SELECT i, s FROM TestEntity"
    }
    "null (not supported)" in {
      val q = quote {
        qr1.filter(t => t.s == null)
      }
      "mirrorSource.run(q)" mustNot compile
    }
  }

  "action" - {
    "insert" in {
      val q = quote {
        qr1.insert
      }
      mirrorSource.run(q)(List()).cql mustEqual
        "INSERT INTO TestEntity (s,i,l,o) VALUES (?, ?, ?, ?)"
    }
    "update" - {
      "all" in {
        val q = quote {
          qr1.update
        }
        mirrorSource.run(q)(List()).cql mustEqual
          "UPDATE TestEntity SET s = ?, i = ?, l = ?, o = ?"
      }
      "filtered" in {
        val q = quote {
          qr1.filter(t => t.i == 1).update
        }
        mirrorSource.run(q)(List()).cql mustEqual
          "UPDATE TestEntity SET s = ?, i = ?, l = ?, o = ? WHERE i = 1"
      }
    }
    "delete" - {
      "filtered" in {
        val q = quote {
          qr1.filter(t => t.i == 1).delete
        }
        mirrorSource.run(q).cql mustEqual
          "DELETE FROM TestEntity WHERE i = 1"
      }
      "all" in {
        val q = quote {
          qr1.delete
        }
        mirrorSource.run(q).cql mustEqual
          "TRUNCATE TestEntity"
      }
      "column" in {
        val q = quote {
          qr1.map(t => t.i).delete
        }
        mirrorSource.run(q).cql mustEqual
          "DELETE i FROM TestEntity"
      }
    }
    "invalid" in {
      import io.getquill.util.Show._
      import CqlIdiom._
      implicit val n = Literal
      val q = quote {
        qr1.update
      }
      intercept[IllegalStateException] {
        q.ast.body.show
      }
      ()
    }
  }

  "infix" - {
    "query" - {
      "partial" in {
        val q = quote {
          qr1.filter(t => infix"${t.i} = 1".as[Boolean])
        }
        mirrorSource.run(q).cql mustEqual
          "SELECT s, i, l, o FROM TestEntity WHERE i = 1"
      }
      "full" in {
        val q = quote {
          infix"SELECT COUNT(1) FROM TestEntity ALLOW FILTERING".as[Query[Int]]
        }
        mirrorSource.run(q).cql mustEqual
          "SELECT COUNT(1) FROM TestEntity ALLOW FILTERING"
      }
    }
    "action" - {
      "partial" in {
        val q = quote {
          qr1.filter(t => infix"${t.i} = 1".as[Boolean]).update
        }
        mirrorSource.run(q)(List()).cql mustEqual
          "UPDATE TestEntity SET s = ?, i = ?, l = ?, o = ? WHERE i = 1"
      }
      "full" in {
        val q = quote {
          infix"TRUNCATE TestEntity".as[Query[Int]]
        }
        mirrorSource.run(q).cql mustEqual
          "TRUNCATE TestEntity"
      }
    }
  }
}
