package io.getquill.context.sql.norm

import io.getquill.MirrorSqlDialectWithReturnClause
import io.getquill.ReturnAction.{ReturnColumns, ReturnRecord}
import io.getquill.context.sql.testContext._
import io.getquill.context.sql.testContext
import io.getquill.Query
import io.getquill.base.Spec

class RenamePropertiesSpec extends Spec {

  case class IntLongCaseClassScope(im: Int, lm: Long)

  val e = quote {
    querySchema[TestEntity]("test_entity", _.s -> "field_s", _.i -> "field_i")
  }

  val e2 = quote {
    querySchema[TestEntity]("test_entity_2", _.s -> "field_s_2", _.i -> "field_i_2")
  }

  val tup = quote {
    querySchema[(String, Int)]("test_tuple", _._1 -> "field_s", _._2 -> "field_i")
  }

  val f = quote {
    qr1.filter(t => t.i == 1)
  }

  // Tables for "nested flatMap in flatJoin" case
  case class Table1(id: String, category: Int, active: Boolean)
  case class Table2(id: String, name: String)
  case class Table3(id: String, configurationId: String, name: String)

  "renames properties of a tuple" - {
    "body" in {
      val q = quote {
        tup.map(t => (t._1, t._2))
      }
      testContext.run(q).string mustEqual
        "SELECT t.field_s AS _1, t.field_i AS _2 FROM test_tuple t"
    }
    "mapped to caseclass and filtered" in {
      case class StringInt(strProp: String, intProp: Int)
      val q = quote {
        tup.map(t => new StringInt(t._1, t._2)).filter(_.strProp == "foo")
      }
      testContext.run(q).string mustEqual
        "SELECT t.field_s AS strProp, t.field_i AS intProp FROM test_tuple t WHERE t.field_s = 'foo'"
    }
  }

  "renames properties according to the entity aliases" - {
    "action" - {
      "insert" in {
        val q = quote {
          e.insertValue(lift(TestEntity("a", 1, 1L, None, true)))
        }
        testContext.run(q).string mustEqual
          "INSERT INTO test_entity (field_s,field_i,l,o,b) VALUES (?, ?, ?, ?, ?)"
      }
      "insert assigned" in {
        val q = quote {
          e.insert(_.i -> lift(1), _.l -> lift(1L), _.o -> lift(Option(1)), _.s -> lift("test"), _.b -> lift(true))
        }
        testContext.run(q).string mustEqual
          "INSERT INTO test_entity (field_i,l,o,field_s,b) VALUES (?, ?, ?, ?, ?)"
      }
      "update" in {
        val q = quote {
          e.filter(t => t.i == 999).updateValue(lift(TestEntity("a", 1, 1L, None, true)))
        }
        testContext.run(q).string mustEqual
          "UPDATE test_entity AS t SET field_s = ?, field_i = ?, l = ?, o = ?, b = ? WHERE t.field_i = 999"
      }
      "delete" in {
        val q = quote {
          e.filter(t => t.i == 999).delete
        }
        testContext.run(q).string mustEqual
          "DELETE FROM test_entity AS t WHERE t.field_i = 999"
      }
      "returning" - {
        "returning - alias" in testContext.withDialect(MirrorSqlDialectWithReturnClause) { ctx =>
          import ctx._
          val e1 = quote {
            querySchema[TestEntity]("test_entity", _.s -> "field_s", _.i -> "field_i")
          }
          val q = quote {
            e1.insertValue(lift(TestEntity("s", 1, 1L, None, true))).returning(_.i)
          }
          val mirror = ctx.run(q)
          mirror.returningBehavior mustEqual ReturnRecord
        }
        "returning generated - alias" in {
          val q = quote {
            e.insertValue(lift(TestEntity("s", 1, 1L, None, true))).returningGenerated(_.i)
          }
          val mirror = testContext.run(q)
          mirror.returningBehavior mustEqual ReturnColumns(List("field_i"))
        }
      }
    }
    "flatMap" - {
      "body" in {
        val q = quote {
          e.flatMap(t => qr2.filter(u => u.s == t.s))
        }
        testContext.run(q).string mustEqual
          "SELECT u.s, u.i, u.l, u.o FROM test_entity t, TestEntity2 u WHERE u.s = t.field_s"
      }
      "transitive" in {
        val q = quote {
          e.flatMap(t => qr2.map(u => t)).map(t => t.s)
        }
        testContext.run(q).string mustEqual
          "SELECT t.field_s FROM test_entity t, TestEntity2 u"
      }
      "with filter" in {
        val q = quote {
          for {
            a <- e
            b <- qr2 if (a.s == b.s)
          } yield {
            (a, b)
          }
        }
        testContext.run(q).string mustEqual
          "SELECT a.field_s, a.field_i, a.l, a.o, a.b, b.s, b.i, b.l, b.o FROM test_entity a, TestEntity2 b WHERE a.field_s = b.s"
      }
    }
    "concatMap" in {
      val q = quote {
        e.concatMap(t => t.s.split(" "))
      }
      testContext.run(q).string mustEqual
        "SELECT UNNEST(SPLIT(t.field_s, ' ')) FROM test_entity t"
    }
    "map" - {
      "body" in {
        val q = quote {
          e.map(t => (t.i, t.l))
        }
        testContext.run(q).string mustEqual
          "SELECT t.field_i AS _1, t.l AS _2 FROM test_entity t"
      }
      "body with caseclass" in {
        case class IntLongCase(im: Int, lm: Long)
        val q = quote {
          e.map(t => new IntLongCase(t.i, t.l))
        }
        testContext.run(q).string mustEqual
          "SELECT t.field_i AS im, t.l AS lm FROM test_entity t"
      }
      "body with caseclass companion constructed" in {
        case class IntLongCase(im: Int, lm: Long)
        val q = quote {
          e.map(t => IntLongCase(t.i, t.l))
        }
        testContext.run(q).string mustEqual
          "SELECT t.field_i AS im, t.l AS lm FROM test_entity t"
      }
      "body with caseclass companion in class scope" in {
        val q = quote {
          e.map(t => IntLongCaseClassScope(t.i, t.l))
        }
        testContext.run(q).string mustEqual
          "SELECT t.field_i AS im, t.l AS lm FROM test_entity t"
      }
      "transitive" in {
        val q = quote {
          e.map(t => t).filter(t => t.i == 1)
        }
        testContext.run(q).string mustEqual
          "SELECT t.field_s, t.field_i, t.l, t.o, t.b FROM test_entity t WHERE t.field_i = 1"
      }
    }
    "union" in {
      val q = quote {
        e.filter(t => t.i == 1).union(e.filter(t => t.i != 1))
      }
      testContext.run(q).string mustEqual
        "(SELECT t.field_s, t.field_i, t.l, t.o, t.b FROM test_entity t WHERE t.field_i = 1) UNION (SELECT t1.field_s, t1.field_i, t1.l, t1.o, t1.b FROM test_entity t1 WHERE t1.field_i <> 1)"
    }
    "unionAll" in {
      val q = quote {
        e.filter(t => t.i == 1).unionAll(e.filter(t => t.i != 1))
      }
      testContext.run(q).string mustEqual
        "(SELECT t.field_s, t.field_i, t.l, t.o, t.b FROM test_entity t WHERE t.field_i = 1) UNION ALL (SELECT t1.field_s, t1.field_i, t1.l, t1.o, t1.b FROM test_entity t1 WHERE t1.field_i <> 1)"
    }
    "filter" - {
      "body" in {
        val q = quote {
          e.filter(t => t.i == 1)
        }
        testContext.run(q).string mustEqual
          "SELECT t.field_s, t.field_i, t.l, t.o, t.b FROM test_entity t WHERE t.field_i = 1"
      }
      "transitive" in {
        val q = quote {
          e.filter(t => t.l == 1).map(t => t.s)
        }
        testContext.run(q).string mustEqual
          "SELECT t.field_s FROM test_entity t WHERE t.l = 1"
      }
    }
    "sortBy" - {
      "body" in {
        val q = quote {
          e.sortBy(t => t.i)
        }
        testContext.run(q).string mustEqual
          "SELECT t.field_s, t.field_i, t.l, t.o, t.b FROM test_entity t ORDER BY t.field_i ASC NULLS FIRST"
      }
      "transitive" in {
        val q = quote {
          e.sortBy(t => t.l).map(t => t.s)
        }
        testContext.run(q).string mustEqual
          "SELECT t.field_s FROM test_entity t ORDER BY t.l ASC NULLS FIRST"
      }
    }
    "take" - {
      "body" in {
        val q = quote {
          e.take(1)
        }
        testContext.run(q).string mustEqual
          "SELECT x.field_s, x.field_i, x.l, x.o, x.b FROM test_entity x LIMIT 1"
      }
      "transitive" in {
        val q = quote {
          e.take(1).map(t => t.s)
        }
        testContext.run(q).string mustEqual
          "SELECT t.field_s FROM test_entity t LIMIT 1"
      }
    }
    "drop" - {
      "body" in {
        val q = quote {
          e.drop(1)
        }
        testContext.run(q).string mustEqual
          "SELECT x.field_s, x.field_i, x.l, x.o, x.b FROM test_entity x OFFSET 1"
      }
      "transitive" in {
        val q = quote {
          e.drop(1).map(t => t.s)
        }
        testContext.run(q).string mustEqual
          "SELECT t.field_s FROM test_entity t OFFSET 1"
      }
    }
    "distinct" - {
      "body" in {
        val q = quote {
          e.distinct
        }
        testContext.run(q).string mustEqual
          "SELECT DISTINCT x.field_s, x.field_i, x.l, x.o, x.b FROM test_entity x"
      }
      "transitive" in {
        val q = quote {
          e.distinct.map(t => t.s)
        }
        testContext.run(q).string mustEqual
          "SELECT t.field_s FROM (SELECT DISTINCT x.field_s, x.field_i, x.l, x.o, x.b FROM test_entity x) AS t"
      }
    }

    "join" - {
      "both sides" in {
        val q = quote {
          e.leftJoin(e).on((a, b) => a.s == b.s).map(t => (t._1.s, t._2.map(_.s)))
        }
        testContext.run(q).string mustEqual
          "SELECT a.field_s AS _1, b.field_s AS _2 FROM test_entity a LEFT JOIN test_entity b ON a.field_s = b.field_s"
      }
      "inner" in {
        val q = quote {
          e.join(f).on((a, b) => a.s == b.s).map(t => t._1.s)
        }
        testContext.run(q).string mustEqual
          "SELECT a.field_s FROM test_entity a INNER JOIN (SELECT t.s FROM TestEntity t WHERE t.i = 1) AS t ON a.field_s = t.s"
      }
      "left" in {
        val q = quote {
          e.leftJoin(f).on((a, b) => a.s == b.s).map(t => t._1.s)
        }
        testContext.run(q).string mustEqual
          "SELECT a.field_s FROM test_entity a LEFT JOIN (SELECT t.s FROM TestEntity t WHERE t.i = 1) AS t ON a.field_s = t.s"
      }
      "right" in {
        val q = quote {
          f.rightJoin(e).on((a, b) => a.s == b.s).map(t => t._2.s)
        }
        testContext.run(q).string mustEqual
          "SELECT b.field_s FROM (SELECT t.s FROM TestEntity t WHERE t.i = 1) AS t RIGHT JOIN test_entity b ON t.s = b.field_s"
      }
      "flat inner" in {
        val q = quote {
          for {
            a <- qr2
            x <- e.join(b => a.s == b.s)
          } yield (x.s, x.i)
        }
        testContext.run(q).string mustEqual
          "SELECT b.field_s AS _1, b.field_i AS _2 FROM TestEntity2 a INNER JOIN test_entity b ON a.s = b.field_s"
      }
      "flat left" in {
        val q = quote {
          for {
            a <- qr2
            x <- e.leftJoin(b => a.s == b.s)
          } yield x.map(x => x.i -> x.s)
        }
        testContext.run(q).string mustEqual
          "SELECT b.field_i AS _1, b.field_s AS _2 FROM TestEntity2 a LEFT JOIN test_entity b ON a.s = b.field_s"
      }
      "flat right" in {
        val q = quote {
          for {
            a <- qr2
            x <- e.rightJoin(b => a.s == b.s)
          } yield x.map(x => x.i -> x.s)
        }
        testContext.run(q).string mustEqual
          "SELECT b.field_i AS _1, b.field_s AS _2 FROM TestEntity2 a RIGHT JOIN test_entity b ON a.s = b.field_s"
      }
      "nested flatMap in flatJoin" in {

        val table1Schema = quote {
          querySchema[Table1]("settings", _.id -> "settings_id")
        }

        val table2Schema = quote {
          for {
            c <- querySchema[Table2]("configuration")
            s <- table1Schema.join(st => st.id == c.id) if s.active
          } yield c
        }

        val q = quote {
          for {
            t3 <- query[Table3]
            c  <- table2Schema.join(ct => ct.id == t3.configurationId)
          } yield (t3, c)
        }

        testContext
          .run(q)
          .string mustEqual "SELECT t3.id, t3.configurationId, t3.name, st.id, st.name FROM Table3 t3 INNER JOIN (SELECT c.id, c.name FROM configuration c INNER JOIN settings st ON st.settings_id = c.id WHERE st.active) AS st ON st.id = t3.configurationId"
      }
    }

    "aggregation" - {
      "groupBy" in {
        val q = quote {
          e.groupBy(a => a.s).map { case (s, eq) =>
            s -> eq.map(_.i).sum
          }
        }
        testContext.run(q).string mustEqual
          "SELECT a.field_s AS _1, SUM(a.field_i) AS _2 FROM test_entity a GROUP BY a.field_s"
      }
    }

    "operation" - {
      "unary" in {
        val q = quote {
          e.filter(a => e.filter(b => b.i > 0).isEmpty).map(_.i)
        }
        testContext.run(q).string mustEqual
          "SELECT a.field_i FROM test_entity a WHERE NOT EXISTS (SELECT b.field_s, b.field_i, b.l, b.o, b.b FROM test_entity b WHERE b.field_i > 0)"
      }
      "binary" in {
        val q = quote {
          e.filter(a => e.filter(b => b.i > 0).isEmpty && a.s == "test").map(_.i)
        }
        testContext.run(q).string mustEqual
          "SELECT a.field_i FROM test_entity a WHERE NOT EXISTS (SELECT b.field_s, b.field_i, b.l, b.o, b.b FROM test_entity b WHERE b.field_i > 0) AND a.field_s = 'test'"
      }
      "query body" in {
        val q = quote {
          e.filter(a => a.i > 0).isEmpty
        }
        testContext.run(q).string mustEqual
          "SELECT NOT EXISTS (SELECT a.field_s, a.field_i, a.l, a.o, a.b FROM test_entity a WHERE a.field_i > 0)"
      }
    }
  }

  "respects the schema definition for embedded" - {
    "query" - {
      "without schema" in {
        case class B(c: Int)
        case class A(b: B)
        testContext.run(query[A]).string mustEqual
          "SELECT x.c FROM A x"
      }
      "with schema" in {
        case class B(c: Int)
        case class A(b: B)
        val q = quote {
          querySchema[A]("A", _.b.c -> "bC")
        }
        testContext.run(q).string mustEqual
          "SELECT x.bC FROM A x"
      }
    }
    "query for Option embedded" - {
      "without schema" in {
        case class B(c1: Int, c2: Int)
        case class A(b: Option[B])
        testContext.run(query[A]).string mustEqual
          "SELECT x.c1, x.c2 FROM A x"
      }
      "with schema" in {
        case class B(c1: Int, c2: Int)
        case class A(b: Option[B])
        val q = quote {
          querySchema[A]("A", _.b.map(_.c1) -> "bC1", _.b.map(_.c2) -> "bC2")
        }
        testContext.run(q).string mustEqual
          "SELECT x.bC1, x.bC2 FROM A x"
      }
    }
    "update" - {
      "without schema" in {
        case class B(c: Int)
        case class A(b: B)
        val q = quote {
          query[A].update(_.b.c -> 1)
        }
        testContext.run(q).string mustEqual
          "UPDATE A SET c = 1"
      }
      "with schema" in {
        case class B(c: Int)
        case class A(b: B)
        val q = quote {
          querySchema[A]("A", _.b.c -> "bC").update(_.b.c -> 1)
        }
        testContext.run(q).string mustEqual
          "UPDATE A SET bC = 1"
      }
    }
    "insert" - {
      "without schema" in {
        case class B(c: Int)
        case class A(b: B)
        val q = quote {
          query[A].insert(_.b.c -> 1)
        }
        testContext.run(q).string mustEqual
          "INSERT INTO A (c) VALUES (1)"
      }
      "with schema" in {
        case class B(c: Int)
        case class A(b: B)
        val q = quote {
          querySchema[A]("A", _.b.c -> "bC").insert(_.b.c -> 1)
        }
        testContext.run(q).string mustEqual
          "INSERT INTO A (bC) VALUES (1)"
      }
    }

    "sql" - {
      case class B(b: Int)
      case class A(u: Long, v: Int, w: B)
      "does not break schema" in {
        val q = quote {
          sql"${querySchema[A]("C", _.v -> "m", _.w.b -> "n")} LIMIT 10".as[Query[A]]
        }

        testContext.run(q).string mustEqual
          "SELECT x.u, x.m, x.n FROM C x LIMIT 10"
      }
      "with filter" in {
        val q = quote {
          sql"${querySchema[A]("C", _.v -> "m", _.w.b -> "n").filter(x => x.v == 1)} LIMIT 10".as[Query[A]]
        }

        testContext.run(q).string mustEqual
          "SELECT x.u, x.m, x.n FROM C x WHERE x.m = 1 LIMIT 10"
      }
    }
  }

}
