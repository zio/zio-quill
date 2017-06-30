package io.getquill.context.orientdb

import io.getquill._

class OrientDBQuerySpec extends Spec {

  val mirrorContext = orientdb.mirrorContext
  import mirrorContext._

  "map" - {
    "property" in {
      val q = quote {
        qr1.map(t => t.i)
      }
      mirrorContext.run(q).string mustEqual
        "SELECT i FROM TestEntity"
    }
    "tuple" in {
      val q = quote {
        qr1.map(t => (t.i, t.s))
      }
      mirrorContext.run(q).string mustEqual
        "SELECT i, s FROM TestEntity"
    }
    "other" in {
      val q = quote {
        qr1.map(t => "s")
      }
      mirrorContext.run(q).string mustEqual
        "SELECT 's' FROM TestEntity"
    }
  }

  "take" in {
    val q = quote {
      qr1.take(1)
    }
    mirrorContext.run(q).string mustEqual
      "SELECT s, i, l, o FROM TestEntity LIMIT 1"
  }

  "sortBy" - {
    "property" in {
      val q = quote {
        qr1.sortBy(t => t.i)
      }
      mirrorContext.run(q).string mustEqual
        "SELECT s, i, l, o FROM TestEntity ORDER BY i ASC"
    }
    "tuple" in {
      val q = quote {
        qr1.sortBy(t => (t.i, t.s))
      }
      mirrorContext.run(q).string mustEqual
        "SELECT s, i, l, o FROM TestEntity ORDER BY i ASC, s ASC"
    }
    "custom ordering" - {
      "property" in {
        val q = quote {
          qr1.sortBy(t => t.i)(Ord.desc)
        }
        mirrorContext.run(q).string mustEqual
          "SELECT s, i, l, o FROM TestEntity ORDER BY i DESC"
      }
      "tuple" in {
        val q = quote {
          qr1.sortBy(t => (t.i, t.s))(Ord(Ord.asc, Ord.desc))
        }
        mirrorContext.run(q).string mustEqual
          "SELECT s, i, l, o FROM TestEntity ORDER BY i ASC, s DESC"
      }
      "tuple single ordering" in {
        val q = quote {
          qr1.sortBy(t => (t.i, t.s))(Ord.desc)
        }
        mirrorContext.run(q).string mustEqual
          "SELECT s, i, l, o FROM TestEntity ORDER BY i DESC, s DESC"
      }
    }
  }

  "filter" in {
    val q = quote {
      qr1.filter(t => t.i == 1)
    }
    mirrorContext.run(q).string mustEqual
      "SELECT s, i, l, o FROM TestEntity WHERE i = 1"
  }

  "entity" in {
    mirrorContext.run(qr1).string mustEqual
      "SELECT s, i, l, o FROM TestEntity"
  }

  "aggregation" - {
    "min" in {
      val q = quote {
        qr1.map(t => t.i).min
      }

      mirrorContext.run(q).string mustEqual
        "SELECT MIN(i) FROM TestEntity"
    }
    "max" in {
      val q = quote {
        qr1.map(t => t.i).max
      }

      mirrorContext.run(q).string mustEqual
        "SELECT MAX(i) FROM TestEntity"
    }
    "sum" in {
      val q = quote {
        qr1.map(t => t.i).sum
      }

      mirrorContext.run(q).string mustEqual
        "SELECT SUM(i) FROM TestEntity"
    }
    "avg" in {
      val q = quote {
        qr1.map(t => t.i).avg
      }
      mirrorContext.run(q).string mustEqual
        "SELECT AVG(i) FROM TestEntity"
    }
    "count" in {
      val q = quote {
        qr1.filter(t => t.i == 1).size
      }
      mirrorContext.run(q).string mustEqual
        "SELECT COUNT(*) FROM TestEntity WHERE i = 1"
    }
  }

  "distinct query" in {
    val q = quote {
      qr1.map(t => t.i).distinct
    }
    mirrorContext.run(q).string mustEqual
      "SELECT DISTINCT(i) FROM TestEntity"
  }

  "all terms" in {
    val q = quote {
      qr1.filter(t => t.i == 1).sortBy(t => t.s).take(1).map(t => t.s)
    }
    mirrorContext.run(q).string mustEqual
      "SELECT s FROM TestEntity WHERE i = 1 ORDER BY s ASC LIMIT 1"
  }

  "groupBy supported" in {
    val q = quote {
      qr1.groupBy(t => t.s).map(t => t._1)
    }
    mirrorContext.run(q).string mustEqual
      "SELECT s FROM TestEntity GROUP BY s"
  }

  "union supported" in {
    val q = quote {
      qr1.filter(_.i == 0).union(qr1.filter(_.i == 1))
    }
    mirrorContext.run(q).string mustEqual
      f"SELECT s, i, l, o FROM (SELECT $$c LET $$a = (SELECT s, i, l, o FROM TestEntity WHERE i = 0), $$b = (SELECT s, i, l, o FROM TestEntity WHERE i = 1), $$c = UNIONALL($$a, $$b))"
  }

  "unionall supported" in {
    val q = quote {
      qr1.filter(_.i == 0).unionAll(qr1.filter(_.i == 1))
    }
    mirrorContext.run(q).string mustEqual
      f"SELECT s, i, l, o FROM (SELECT $$c LET $$a = (SELECT s, i, l, o FROM TestEntity WHERE i = 0), $$b = (SELECT s, i, l, o FROM TestEntity WHERE i = 1), $$c = UNIONALL($$a, $$b))"
  }
}