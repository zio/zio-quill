package io.getquill.sources.sql.norm

import io.getquill.sources.sql.mirrorSource
import io.getquill._

class RenamePropertiesSpec extends Spec {

  val e = quote {
    query[TestEntity]("test_entity", _.s -> "field_s", _.i -> "field_i")
  }

  val f = quote {
    qr1.filter(t => t.i == 1)
  }

  "renames properties according to the entity aliases" - {
    "flatMap" - {
      "body" in {
        val q = quote {
          e.flatMap(t => qr2.filter(u => u.s == t.s))
        }
        mirrorSource.run(q).sql mustEqual
          "SELECT u.s, u.i, u.l, u.o FROM test_entity t, TestEntity2 u WHERE u.s = t.field_s"
      }
      "transitive" in {
        val q = quote {
          e.flatMap(t => qr2.map(u => t)).map(t => t.s)
        }
        mirrorSource.run(q).sql mustEqual
          "SELECT t.field_s FROM test_entity t, TestEntity2 u"
      }
    }
    "map" - {
      "body" in {
        val q = quote {
          e.map(t => (t.i, t.l))
        }
        mirrorSource.run(q).sql mustEqual
          "SELECT t.field_i, t.l FROM test_entity t"
      }
      "transitive" in {
        val q = quote {
          e.map(t => t).filter(t => t.i == 1)
        }
        mirrorSource.run(q).sql mustEqual
          "SELECT t.field_s, t.field_i, t.l, t.o FROM test_entity t WHERE t.field_i = 1"
      }
    }
    "filter" - {
      "body" in {
        val q = quote {
          e.filter(t => t.i == 1)
        }
        mirrorSource.run(q).sql mustEqual
          "SELECT t.field_s, t.field_i, t.l, t.o FROM test_entity t WHERE t.field_i = 1"
      }
      "transitive" in {
        val q = quote {
          e.filter(t => t.l == 1).map(t => t.s)
        }
        mirrorSource.run(q).sql mustEqual
          "SELECT t.field_s FROM test_entity t WHERE t.l = 1"
      }
    }
    "sortBy" - {
      "body" in {
        val q = quote {
          e.sortBy(t => t.i)
        }
        mirrorSource.run(q).sql mustEqual
          "SELECT t.field_s, t.field_i, t.l, t.o FROM test_entity t ORDER BY t.field_i ASC NULLS FIRST"
      }
      "transitive" in {
        val q = quote {
          e.sortBy(t => t.l).map(t => t.s)
        }
        mirrorSource.run(q).sql mustEqual
          "SELECT t.field_s FROM test_entity t ORDER BY t.l ASC NULLS FIRST"
      }
    }
    "join" - {
      "both sides" in {
        val q = quote {
          e.leftJoin(e).on((a, b) => a.s == b.s).map(t => (t._1.s, t._2.map(_.s)))
        }
        mirrorSource.run(q).sql mustEqual
          "SELECT a.field_s, b.field_s FROM test_entity a LEFT JOIN test_entity b ON a.field_s = b.field_s"
      }
      "inner" in {
        val q = quote {
          e.join(f).on((a, b) => a.s == b.s).map(t => t._1.s)
        }
        mirrorSource.run(q).sql mustEqual
          "SELECT a.field_s FROM test_entity a INNER JOIN (SELECT t.s FROM TestEntity t WHERE t.i = 1) t ON a.field_s = t.s"
      }
      "left" in {
        val q = quote {
          e.leftJoin(f).on((a, b) => a.s == b.s).map(t => t._1.s)
        }
        mirrorSource.run(q).sql mustEqual
          "SELECT a.field_s FROM test_entity a LEFT JOIN (SELECT t.s FROM TestEntity t WHERE t.i = 1) t ON a.field_s = t.s"
      }
      "right" in {
        val q = quote {
          f.rightJoin(e).on((a, b) => a.s == b.s).map(t => t._2.s)
        }
        mirrorSource.run(q).sql mustEqual
          "SELECT b.field_s FROM (SELECT t.s FROM TestEntity t WHERE t.i = 1) t RIGHT JOIN test_entity b ON t.s = b.field_s"
      }
    }
  }

}
