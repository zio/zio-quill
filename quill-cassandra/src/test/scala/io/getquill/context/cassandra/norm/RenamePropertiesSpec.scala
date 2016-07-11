package io.getquill.context.cassandra.norm

import io.getquill._
import io.getquill.context.cassandra.mirrorContext

class RenamePropertiesSpec extends Spec {

  import mirrorContext._

  val e = quote {
    query[TestEntity].schema(_.entity("test_entity").columns(_.s -> "field_s", _.i -> "field_i"))
  }

  val f = quote {
    qr1.filter(t => t.i == 1)
  }

  "renames properties according to the entity aliases" - {
    "action" - {
      "insert" in {
        val q = quote {
          e.insert
        }
        mirrorContext.run(q)(TestEntity("a", 1, 1L, None)).cql mustEqual
          "INSERT INTO test_entity (field_s,field_i,l,o) VALUES (?, ?, ?, ?)"
      }

      "insert assigned" in {
        val q = quote {
          e.insert(_.i -> 1, _.l -> 1L, _.o -> 1, _.s -> "test")
        }
        mirrorContext.run(q).cql mustEqual
          "INSERT INTO test_entity (field_i,l,o,field_s) VALUES (1, 1, 1, 'test')"
      }
      "update" in {
        val q = quote {
          e.filter(_.i == 999).update
        }
        mirrorContext.run(q)(TestEntity("a", 1, 1L, None)).cql mustEqual
          "UPDATE test_entity SET field_s = ?, field_i = ?, l = ?, o = ? WHERE field_i = 999"
      }
      "delete" in {
        val q: Quoted[Delete[TestEntity, Long]] = quote {
          e.filter(_.i == 999).delete
        }
        mirrorContext.run(q).cql mustEqual
          "DELETE FROM test_entity WHERE field_i = 999"
      }
    }

    "map" - {
      "body" in {
        val q = quote {
          e.map(t => (t.i, t.l))
        }
        mirrorContext.run(q).cql mustEqual
          "SELECT field_i, l FROM test_entity"
      }
      "transitive" in {
        val q = quote {
          e.map(t => t).filter(t => t.i == 1)
        }
        mirrorContext.run(q).cql mustEqual
          "SELECT field_s, field_i, l, o FROM test_entity WHERE field_i = 1"
      }
    }
    "filter" - {
      "body" in {
        val q = quote {
          e.filter(t => t.i == 1)
        }
        mirrorContext.run(q).cql mustEqual
          "SELECT field_s, field_i, l, o FROM test_entity WHERE field_i = 1"
      }
      "transitive" in {
        val q = quote {
          e.filter(t => t.l == 1).map(t => t.s)
        }
        mirrorContext.run(q).cql mustEqual
          "SELECT field_s FROM test_entity WHERE l = 1"
      }
    }
    "sortBy" - {
      "body" in {
        val q = quote {
          e.sortBy(t => t.i)
        }
        mirrorContext.run(q).cql mustEqual
          "SELECT field_s, field_i, l, o FROM test_entity ORDER BY field_i ASC"
      }
      "transitive" in {
        val q = quote {
          e.sortBy(t => t.l).map(t => t.s)
        }
        mirrorContext.run(q).cql mustEqual
          "SELECT field_s FROM test_entity ORDER BY l ASC"
      }
    }
  }
}
