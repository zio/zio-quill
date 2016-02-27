package io.getquill.sources.cassandra

import io.getquill.util.Show._
import io.getquill._
import io.getquill.naming.Literal
import io.getquill.ast.Ast

class CqlQuerySpec extends Spec {

  import CqlIdiom._

  implicit val naming = Literal

  "map" - {
    "property" in {
      val q = quote {
        qr1.map(t => t.i)
      }
      (q.ast: Ast).show mustEqual
        "SELECT i FROM TestEntity"
    }
    "tuple" in {
      val q = quote {
        qr1.map(t => (t.i, t.s))
      }
      (q.ast: Ast).show mustEqual
        "SELECT i, s FROM TestEntity"
    }
    "other (not supported)" in {
      val q = quote {
        qr1.map(t => "s")
      }
      intercept[IllegalStateException] {
        CqlQuery(q.ast)
      }
      ()
    }
  }

  "take" in {
    val q = quote {
      qr1.take(1)
    }
    (q.ast: Ast).show mustEqual
      "SELECT * FROM TestEntity LIMIT 1"
  }

  "sortBy" - {
    "property" in {
      val q = quote {
        qr1.sortBy(t => t.i)
      }
      (q.ast: Ast).show mustEqual
        "SELECT * FROM TestEntity ORDER BY i ASC"
    }
    "tuple" in {
      val q = quote {
        qr1.sortBy(t => (t.i, t.s))
      }
      (q.ast: Ast).show mustEqual
        "SELECT * FROM TestEntity ORDER BY i ASC, s ASC"
    }
    "custom ordering" - {
      "property" in {
        val q = quote {
          qr1.sortBy(t => t.i)(Ord.desc)
        }
        (q.ast: Ast).show mustEqual
          "SELECT * FROM TestEntity ORDER BY i DESC"
      }
      "tuple" in {
        val q = quote {
          qr1.sortBy(t => (t.i, t.s))(Ord(Ord.asc, Ord.desc))
        }
        (q.ast: Ast).show mustEqual
          "SELECT * FROM TestEntity ORDER BY i ASC, s DESC"
      }
      "tuple single ordering" in {
        val q = quote {
          qr1.sortBy(t => (t.i, t.s))(Ord.desc)
        }
        (q.ast: Ast).show mustEqual
          "SELECT * FROM TestEntity ORDER BY i DESC, s DESC"
      }
      "invalid ordering" in {
        case class Test(a: (Int, Int))
        val q = quote {
          query[Test].sortBy(_.a)(Ord(Ord.asc, Ord.desc))
        }
        intercept[IllegalStateException] {
          CqlQuery(q.ast)
        }
        ()
      }
    }
  }

  "filter" in {
    val q = quote {
      qr1.filter(t => t.i == 1)
    }
    (q.ast: Ast).show mustEqual
      "SELECT * FROM TestEntity WHERE i = 1"
  }

  "entity" in {
    (qr1.ast: Ast).show mustEqual
      "SELECT * FROM TestEntity"
  }

  "aggregation" - {
    "count" in {
      val q = quote {
        qr1.filter(t => t.i == 1).size
      }

      (q.ast: Ast).show mustEqual
        "SELECT COUNT(1) FROM TestEntity WHERE i = 1"
    }
  }

  "distinct query" in {
    val q = quote {
      qr1.map(t => t.i).distinct
    }
    CqlQuery(q.ast).show mustEqual
      "SELECT DISTINCT i FROM TestEntity"
  }

  "all terms" in {
    val q = quote {
      qr1.filter(t => t.i == 1).sortBy(t => t.s).take(1).map(t => t.s)
    }
    (q.ast: Ast).show mustEqual
      "SELECT s FROM TestEntity WHERE i = 1 ORDER BY s ASC LIMIT 1"
  }

  "invalid cql" - {
    "groupBy not supported" in {
      val q = quote {
        qr1.groupBy(t => t.i)
      }
      trap(CqlQuery(q.ast)).getMessage mustEqual "Cql doesn't support GROUP BY."
    }
    "union not supported" in {
      val q = quote {
        qr1.filter(_.i == 0).union(qr1.filter(_.i == 1))
      }
      trap(CqlQuery(q.ast)).getMessage mustEqual "Cql doesn't support UNION/UNION ALL."
    }
    "unionAll not supported" in {
      val q = quote {
        qr1.filter(_.i == 0).unionAll(qr1.filter(_.i == 1))
      }
      trap(CqlQuery(q.ast)).getMessage mustEqual "Cql doesn't support UNION/UNION ALL."
    }
    "join not supported" in {
      val q = quote {
        qr1.join(qr2).on((a, b) => a.i == b.i)
      }
      trap(CqlQuery(q.ast)).getMessage mustEqual "Cql doesn't support JOIN."
    }
    "sortBy after take" in {
      val q = quote {
        qr1.take(1).sortBy(t => t.s)
      }
      intercept[IllegalStateException] {
        CqlQuery(q.ast)
      }
      ()
    }
    "filter after sortBy" in {
      val q = quote {
        qr1.sortBy(t => t.s).filter(t => t.i == 1)
      }
      intercept[IllegalStateException] {
        CqlQuery(q.ast)
      }
      ()
    }
    "filter after take" in {
      val q = quote {
        qr1.take(1).filter(t => t.i == 1)
      }
      intercept[IllegalStateException] {
        CqlQuery(q.ast)
      }
      ()
    }
    "map after distinct" in {
      val q = quote {
        qr1.distinct.map(_.s)
      }
      intercept[IllegalStateException] {
        CqlQuery(q.ast)
      }
      ()
    }
    "size after distinct" in {
      val q = quote {
        qr1.distinct.size
      }
      intercept[IllegalStateException] {
        CqlQuery(q.ast)
      }
      ()
    }
    "take after distinct" in {
      val q = quote {
        qr1.distinct.take(10)
      }
      intercept[IllegalStateException] {
        CqlQuery(q.ast)
      }
      ()
    }
    "sortBy after distinct" in {
      val q = quote {
        qr1.distinct.sortBy(_.i)
      }
      intercept[IllegalStateException] {
        CqlQuery(q.ast)
      }
      ()
    }
    "filter after distinct" in {
      val q = quote {
        qr1.distinct.filter(_.i == 0)
      }
      intercept[IllegalStateException] {
        CqlQuery(q.ast)
      }
      ()
    }
  }
}
