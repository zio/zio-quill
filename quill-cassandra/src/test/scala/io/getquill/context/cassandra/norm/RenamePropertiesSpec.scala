package io.getquill.context.cassandra.norm

import io.getquill._
import io.getquill.context.cassandra.mirrorContext

class RenamePropertiesSpec extends Spec {

  import mirrorContext._

  val e = quote {
    querySchema[TestEntity]("test_entity", _.s -> "field_s", _.i -> "field_i")
  }

  val f = quote {
    qr1.filter(t => t.i == 1)
  }

  "renames properties according to the entity aliases" - {

    "allowFiltering" in {
      val q = quote {
        e.filter(_.i == 1).allowFiltering
      }
      mirrorContext.run(q).string mustEqual
        "SELECT field_s, field_i, l, o, b FROM test_entity WHERE field_i = 1 ALLOW FILTERING"
    }

    "action" - {
      "insert" in {
        val q = quote {
          e.insert(lift(TestEntity("a", 1, 1L, None, true)))
        }
        mirrorContext.run(q).string mustEqual
          "INSERT INTO test_entity (field_s,field_i,l,o,b) VALUES (?, ?, ?, ?, ?)"
      }

      "insert assigned" in {
        val q = quote {
          e.insert(_.i -> lift(1), _.l -> lift(1L), _.o -> lift(Option(1)), _.s -> lift("test"), _.b -> lift(true))
        }
        mirrorContext.run(q).string mustEqual
          "INSERT INTO test_entity (field_i,l,o,field_s,b) VALUES (?, ?, ?, ?, ?)"
      }
      "update" in {
        val q = quote {
          e.filter(_.i == 999).update(lift(TestEntity("a", 1, 1L, None, true)))
        }
        mirrorContext.run(q).string mustEqual
          "UPDATE test_entity SET field_s = ?, field_i = ?, l = ?, o = ?, b = ? WHERE field_i = 999"
      }
      "delete" in {
        val q: Quoted[Delete[TestEntity]] = quote {
          e.filter(_.i == 999).delete
        }
        mirrorContext.run(q).string mustEqual
          "DELETE FROM test_entity WHERE field_i = 999"
      }
    }

    "map" - {
      "body" in {
        val q = quote {
          e.map(t => (t.i, t.l))
        }
        mirrorContext.run(q).string mustEqual
          "SELECT field_i, l FROM test_entity"
      }
      "transitive" in {
        val q = quote {
          e.map(t => t).filter(t => t.i == 1)
        }
        mirrorContext.run(q).string mustEqual
          "SELECT field_s, field_i, l, o, b FROM test_entity WHERE field_i = 1"
      }
    }
    "filter" - {
      "body" in {
        val q = quote {
          e.filter(t => t.i == 1)
        }
        mirrorContext.run(q).string mustEqual
          "SELECT field_s, field_i, l, o, b FROM test_entity WHERE field_i = 1"
      }
      "transitive" in {
        val q = quote {
          e.filter(t => t.l == 1).map(t => t.s)
        }
        mirrorContext.run(q).string mustEqual
          "SELECT field_s FROM test_entity WHERE l = 1"
      }
    }
    "sortBy" - {
      "body" in {
        val q = quote {
          e.sortBy(t => t.i)
        }
        mirrorContext.run(q).string mustEqual
          "SELECT field_s, field_i, l, o, b FROM test_entity ORDER BY field_i ASC"
      }
      "transitive" in {
        val q = quote {
          e.sortBy(t => t.l).map(t => t.s)
        }
        mirrorContext.run(q).string mustEqual
          "SELECT field_s FROM test_entity ORDER BY l ASC"
      }
    }
  }
}
