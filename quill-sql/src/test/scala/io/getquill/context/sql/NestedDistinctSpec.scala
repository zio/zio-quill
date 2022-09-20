package io.getquill.context.sql

import io.getquill.base.Spec
import io.getquill.{Literal, MirrorSqlDialect, SqlMirrorContext}
import io.getquill.context.sql.util.StringOps._

class NestedDistinctSpec extends Spec {
  val v = "foo"

  "nested distinct clauses should" - {

    val ctx = new SqlMirrorContext(MirrorSqlDialect, Literal)
    import ctx._

    "first operation" - {
      case class MyEmb(name: String)
      case class MyParent(myEmb: MyEmb)

      "first operation nesting with filter" in {
        val q = quote {
          query[MyParent].distinct
            .filter(_.myEmb.name == "test")
        }

        ctx.run(q).string mustEqual "SELECT DISTINCT x1.name FROM MyParent x1 WHERE x1.name = 'test'"
      }

      "first operation nesting with filter and outer nesting" in {
        val q = quote {
          query[MyParent].distinct
            .filter(f => f.myEmb.name == "test")
            .nested
        }

        ctx.run(q).string mustEqual "SELECT x.myEmbname AS name FROM (SELECT DISTINCT f.name AS myEmbname FROM MyParent f WHERE f.name = 'test') AS x"
      }

      "first operation nesting with filter before and after" in {
        val q = quote {
          query[MyParent]
            .filter(_.myEmb.name == "firstTest")
            .distinct
            .filter(_.myEmb.name == "test")
        }

        ctx.run(q).string mustEqual "SELECT x3.myEmbname AS name FROM (SELECT DISTINCT x2.name AS myEmbname FROM MyParent x2 WHERE x2.name = 'firstTest') AS x3 WHERE x3.myEmbname = 'test'"
      }

      "first operation nesting with filter before and after - groupBy" in { // hello
        case class MyEmb(name: Int)
        case class MyParent(myEmb: MyEmb)

        val q = quote {
          query[MyParent]
            .filter(_.myEmb.name == 1)
            .distinct
            .filter(_.myEmb.name == 2)
            .groupBy(p => p.myEmb.name)
            .map(tup => tup._2.map(_.myEmb.name).sum)
        }

        ctx.run(q).string mustEqual "SELECT SUM(x5.myEmbname) FROM (SELECT DISTINCT x4.name AS myEmbname FROM MyParent x4 WHERE x4.name = 1) AS x5 WHERE x5.myEmbname = 2 GROUP BY x5.myEmbname"
      }

      "first operation nesting with filter before and after - orderBy" in {
        val q = quote {
          query[MyParent]
            .filter(_.myEmb.name == "firstTest")
            .distinct
            .filter(_.myEmb.name == "test")
            .sortBy(p => p.myEmb.name)
        }

        ctx.run(q).string mustEqual "SELECT x8.myEmbname AS name FROM (SELECT DISTINCT x7.name AS myEmbname FROM MyParent x7 WHERE x7.name = 'firstTest') AS x8 WHERE x8.myEmbname = 'test' ORDER BY x8.myEmbname ASC NULLS FIRST"
      }

      "first operation nesting with filter before and after - limit" in {
        val q = quote {
          query[MyParent]
            .filter(_.myEmb.name == "firstTest")
            .distinct
            .filter(_.myEmb.name == "test")
            .take(7)
        }

        ctx.run(q).string mustEqual "SELECT x10.myEmbname AS name FROM (SELECT DISTINCT x9.name AS myEmbname FROM MyParent x9 WHERE x9.name = 'firstTest') AS x10 WHERE x10.myEmbname = 'test' LIMIT 7"
      }

      "first operation nesting with filter before and after - offset" in {
        val q = quote {
          query[MyParent]
            .filter(_.myEmb.name == "firstTest")
            .distinct
            .filter(_.myEmb.name == "test")
            .drop(7)
        }

        ctx.run(q).string mustEqual "SELECT x12.myEmbname AS name FROM (SELECT DISTINCT x11.name AS myEmbname FROM MyParent x11 WHERE x11.name = 'firstTest') AS x12 WHERE x12.myEmbname = 'test' OFFSET 7"
      }

      "first operation nesting with filter - nested" in {
        val q = quote {
          query[MyParent].nested
            .filter(_.myEmb.name == "test")
        }

        ctx.run(q).string mustEqual "SELECT x13.myEmbname AS name FROM (SELECT x.name AS myEmbname FROM MyParent x) AS x13 WHERE x13.myEmbname = 'test'"
      }

      "first operation nesting with filter before and after - nested" in {
        val q = quote {
          query[MyParent]
            .filter(_.myEmb.name == "firstTest")
            .nested
            .filter(_.myEmb.name == "test")
        }

        ctx.run(q).string mustEqual "SELECT x15.myEmbname AS name FROM (SELECT x14.name AS myEmbname FROM MyParent x14 WHERE x14.name = 'firstTest') AS x15 WHERE x15.myEmbname = 'test'"
      }
    }

    "works with querySchema" in {
      case class SimpleEnt(a: Int, b: String)
      case class SimpleEnt2(aa: Int, bb: String)

      val qschem = quote {
        querySchema[SimpleEnt]("CustomEnt", _.a -> "field_a")
      }

      val q = quote {
        qschem
          .map(e => SimpleEnt(e.a + 1, e.b))
          .distinct
          .map(e => SimpleEnt2(e.a + 2, e.b))
          .distinct
      }
      ctx.run(q).string mustEqual "SELECT DISTINCT e.a + 2 AS aa, e.b AS bb FROM (SELECT DISTINCT e.field_a + 1 AS a, e.b FROM CustomEnt e) AS e"
    }

    "works with explicitly nested infixes" in {
      case class SimpleEnt(a: Int, b: String)
      case class SimpleEnt2(aa: Int, bb: String)

      val qschem = quote {
        querySchema[SimpleEnt]("CustomEnt", _.a -> "field_a")
      }

      val q = quote {
        qschem
          .map(e => (e.a + 1, sql"foo(${e.b})".as[String]))
          .nested
          .map(e => (e._1 + 2, sql"bar(${e._2})".as[String]))
          .nested
      }

      ctx.run(q).string mustEqual "SELECT x._1, x._2 FROM (SELECT e._1 + 2 AS _1, bar(e._2) AS _2 FROM (SELECT e.field_a + 1 AS _1, foo(e.b) AS _2 FROM CustomEnt e) AS e) AS x"
    }

    "works with implicitly nested infixes" in {
      case class SimpleEnt(a: Int, b: String)
      case class SimpleEnt2(aa: Int, bb: String)

      val qschem = quote {
        querySchema[SimpleEnt]("CustomEnt", _.a -> "field_a")
      }

      val q = quote {
        qschem
          .map(e => (e.a + 1, sql"foo(${e.b})".as[String]))
          .map(e => (e._1 + 2, sql"bar(${e._2})".as[String]))
      }

      ctx.run(q).string mustEqual
        "SELECT e._1 + 2 AS _1, bar(e._2) AS _2 FROM (SELECT e.field_a + 1 AS _1, foo(e.b) AS _2 FROM CustomEnt e) AS e"
    }

    "embedded entity from parent" - {
      case class Emb(id: Int, name: String)
      case class Parent(idP: Int, emb: Emb)
      implicit val parentMeta = schemaMeta[Parent]("Parent", _.emb.name -> "theName")

      "embedded can be propagated across distinct inside tuple with naming intact" in {
        val q = quote {
          query[Parent].map(p => (p.emb, 1)).distinct.map(e => (e._1.name, e._1.id))
        }

        ctx.run(q).string mustEqual "SELECT p._1theName AS _1, p._1id AS _2 FROM (SELECT DISTINCT p.id AS _1id, p.theName AS _1theName, 1 AS _2 FROM Parent p) AS p"
      }

      "embedded can be propagated across distinct inside case class with naming intact" in {
        case class SuperParent(emb: Emb, id: Int)

        val q = quote {
          query[Parent].map(p => SuperParent(p.emb, 1)).distinct.map(e => (e.emb.name, e.emb.id))
        }

        ctx.run(q).string mustEqual "SELECT p.embtheName AS _1, p.embid AS _2 FROM (SELECT DISTINCT p.id AS embid, p.theName AS embtheName, 1 AS id FROM Parent p) AS p"
      }

      "can be propagated across query with naming intact" in {
        val q = quote {
          query[Parent].map(p => p.emb).nested.map(e => (e.name, e.id))
        }
        ctx
          .run(q)
          .string mustEqual "SELECT e.theName AS _1, e.id AS _2 FROM (SELECT p.id, p.theName FROM Parent p) AS e"
      }

      "can be propagated across query with naming intact - embedded and column" in {
        val q = quote {
          query[Parent].map(p => (p.idP, p.emb)).nested.map(e => (e._1, e._2))
        }
        ctx.run(q).string mustEqual "SELECT e._1, e._2id AS id, e._2theName AS theName FROM (SELECT p.idP AS _1, p.id AS _2id, p.theName AS _2theName FROM Parent p) AS e"
      }

      "can be propagated across query with naming intact and then used further" in {
        val q = quote {
          query[Parent].map(p => p.emb).distinct.map(e => (e.name, e.id)).distinct.map(tup => (tup._1, tup._2)).distinct
        }
        ctx.run(q).string mustEqual "SELECT DISTINCT p._1theName AS _1, p._1id AS _2 FROM (SELECT DISTINCT p.id AS _1id, p.theName AS _1theName FROM Parent p) AS p"
      }

      "can be propagated across query with naming intact and then used further - nested" in {
        val q = quote {
          query[Parent].map(p => p.emb).nested.map(e => (e.name, e.id)).nested.map(tup => (tup._1, tup._2)).nested
        }
        ctx.run(q).string mustEqual "SELECT x._1, x._2 FROM (SELECT tup._1, tup._2 FROM (SELECT e.theName AS _1, e.id AS _2 FROM (SELECT p.id, p.theName FROM Parent p) AS e) AS tup) AS x"
      }

      "can be propagated across query with naming intact - returned as single property" in {
        val q = quote {
          query[Parent].map(p => p.emb).distinct.map(e => (e.name))
        }
        ctx.run(q).string mustEqual "SELECT p._1theName AS theName FROM (SELECT DISTINCT p.id AS _1id, p.theName AS _1theName FROM Parent p) AS p"
      }

      "can be propagated across query with naming intact - and the immediately returned" in {
        val q = quote {
          query[Parent].map(p => p.emb).nested.map(e => e)
        }
        ctx.run(q).string mustEqual "SELECT x.id, x.theName FROM (SELECT p.id, p.theName FROM Parent p) AS x"
      }

      "can be propagated across distinct with naming intact - and the immediately returned" in {
        val q = quote {
          query[Parent].map(p => p.emb).distinct.map(e => e)
        }
        ctx.run(q).string mustEqual "SELECT DISTINCT p.id, p.theName FROM Parent p"
      }

      "can be propagated across query with naming intact and then re-wrapped in case class" in {
        val q = quote {
          query[Parent].map(p => p.emb).distinct.map(e => Parent(1, e))
        }
        ctx.run(q).string mustEqual "SELECT 1 AS idP, p._1id AS id, p._1theName AS theName FROM (SELECT DISTINCT p.id AS _1id, p.theName AS _1theName FROM Parent p) AS p"
      }

      "can be propagated across query with naming intact and then re-wrapped in tuple" in {
        val q = quote {
          query[Parent].map(p => p.emb).nested.map(e => Parent(1, e))
        }
        ctx.run(q).string mustEqual "SELECT 1 AS idP, e.id, e.theName FROM (SELECT p.id, p.theName FROM Parent p) AS e"
      }
    }

    "double embedded entity from parent" - {
      case class Emb(id: Int, name: String)
      case class Parent(id: Int, name: String, emb: Emb)
      case class GrandParent(id: Int, par: Parent)
      implicit val parentMeta =
        schemaMeta[GrandParent]("GrandParent", _.par.emb.name -> "theName", _.par.name -> "theParentName")

      "fully unwrapped name propagates" in {
        val q = quote {
          query[GrandParent]
            .map(g => g.par)
            .distinct
            .map(p => p.emb)
            .map(p => p.name)
            .distinct
        }
        ctx.run(q).string mustEqual "SELECT DISTINCT g._1embtheName AS theName FROM (SELECT DISTINCT g.id AS _1id, g.theParentName AS _1theParentName, g.id AS _1embid, g.theName AS _1embtheName FROM GrandParent g) AS g"
      }

      "fully unwrapped name propagates with side property" in {
        val q = quote {
          query[GrandParent]
            .map(g => g.par)
            .distinct
            .map(p => (p.name, p.emb))
            .distinct
            .map(tup => (tup._1, tup._2))
            .distinct
        }
        ctx.run(q).string mustEqual
          "SELECT DISTINCT g._1theParentName AS _1, g._1embid AS id, g._1embtheName AS theName FROM (SELECT DISTINCT g.id AS _1id, g.theParentName AS _1theParentName, g.id AS _1embid, g.theName AS _1embtheName FROM GrandParent g) AS g"
      }

      "fully unwrapped name propagates with side property - nested" in {
        val q = quote {
          query[GrandParent]
            .map(g => g.par)
            .nested
            .map(p => (p.name, p.emb))
            .nested
            .map(tup => (tup._1, tup._2))
            .nested
        }
        println(ctx.run(q).string(true))
        ctx.run(q).string(true).collapseSpace mustEqual
          """
            |SELECT
            |  x._1,
            |  x._2id AS id,
            |  x._2theName AS theName
            |FROM
            |  (
            |    SELECT
            |      tup._1,
            |      tup._2id,
            |      tup._2theName
            |    FROM
            |      (
            |        SELECT
            |          p.theParentName AS _1,
            |          p.embid AS _2id,
            |          p.embtheName AS _2theName
            |        FROM
            |          (
            |            SELECT
            |              g.theParentName,
            |              g.id AS embid,
            |              g.theName AS embtheName
            |            FROM
            |              GrandParent g
            |          ) AS p
            |      ) AS tup
            |  ) AS x
            |""".collapseSpace
      }

      "fully unwrapped name propagates with un-renamed properties" in {
        val q = quote {
          query[GrandParent]
            .map(g => g.par)
            .distinct
            .map(p => (p.name, p.emb, p.id, p.emb.id))
            .distinct
            .map(tup => (tup._1, tup._2, tup._3, tup._4))
            .distinct
        }
        ctx.run(q).string(true).collapseSpace mustEqual //
          """
            |SELECT
            |  DISTINCT g._1theParentName AS _1,
            |  g._1embid AS id,
            |  g._1embtheName AS theName,
            |  g._1id AS _3,
            |  g._1embid AS _4
            |FROM
            |  (
            |    SELECT
            |      DISTINCT g.id AS _1id,
            |      g.theParentName AS _1theParentName,
            |      g.id AS _1embid,
            |      g.theName AS _1embtheName
            |    FROM
            |      GrandParent g
            |  ) AS g
            |""".collapseSpace
      }

      "fully unwrapped name propagates with un-renamed properties - with one property renamed" in {
        implicit val parentMeta = schemaMeta[GrandParent](
          "GrandParent",
          _.id           -> "gId",
          _.par.emb.name -> "theName",
          _.par.name     -> "theParentName"
        )
        val q = quote {
          query[GrandParent]
            .map(g => g.par)
            .distinct
            .map(p => (p.name, p.emb, p.id, p.emb.id))
            .distinct
            .map(tup => (tup._1, tup._2, tup._3, tup._4))
            .distinct
        }
        ctx.run(q).string(true).collapseSpace mustEqual
          """
            |SELECT
            |  DISTINCT g._1theParentName AS _1,
            |  g._1embid AS id,
            |  g._1embtheName AS theName,
            |  g._1id AS _3,
            |  g._1embid AS _4
            |FROM
            |  (
            |    SELECT
            |      DISTINCT g.id AS _1id,
            |      g.theParentName AS _1theParentName,
            |      g.id AS _1embid,
            |      g.theName AS _1embtheName
            |    FROM
            |      GrandParent g
            |  ) AS g
            |""".collapseSpace
      }

      "fully unwrapped and fully re-wrapped" in {
        implicit val parentMeta =
          schemaMeta[GrandParent]("GrandParent", _.par.emb.name -> "theName", _.par.name -> "theParentName")
        val q = quote {
          query[GrandParent]
            .map(g => (g.id, g.par))
            .distinct
            .map(p => (p._1, p._2.id, p._2.name, p._2.emb))
            .distinct
            .map(tup => (tup._1, tup._2, tup._3, tup._4.id, tup._4.name))
            .distinct
            .map(tup => (tup._1, tup._2, tup._3, tup._4, tup._5))
            .distinct
            .map(tup => (tup._1, tup._2, tup._3, Emb(tup._4, tup._5)))
            .distinct
            .map(tup => (tup._1, Parent(tup._2, tup._3, tup._4)))
            .distinct
            .map(tup => GrandParent(tup._1, tup._2))

        }
        ctx.run(q).string(true).collapseSpace mustEqual
          """
            |SELECT
            |  tup._1 AS id,
            |  tup._2id AS id,
            |  tup._2name AS name,
            |  tup._2embid AS id,
            |  tup._2embname AS name
            |FROM
            |  (
            |    SELECT
            |      DISTINCT tup._1,
            |      tup._2 AS _2id,
            |      tup._3 AS _2name,
            |      tup._4id AS _2embid,
            |      tup._4name AS _2embname
            |    FROM
            |      (
            |        SELECT
            |          DISTINCT tup._1,
            |          tup._2,
            |          tup._3,
            |          tup._4 AS _4id,
            |          tup._5 AS _4name
            |        FROM
            |          (
            |            SELECT
            |              DISTINCT tup._1,
            |              tup._2,
            |              tup._3,
            |              tup._4id AS _4,
            |              tup._4theName AS _5
            |            FROM
            |              (
            |                SELECT
            |                  DISTINCT p._1,
            |                  p._2id AS _2,
            |                  p._2theParentName AS _3,
            |                  p._2embid AS _4id,
            |                  p._2embtheName AS _4theName
            |                FROM
            |                  (
            |                    SELECT
            |                      DISTINCT g.id AS _1,
            |                      g.id AS _2id,
            |                      g.theParentName AS _2theParentName,
            |                      g.id AS _2embid,
            |                      g.theName AS _2embtheName
            |                    FROM
            |                      GrandParent g
            |                  ) AS p
            |              ) AS tup
            |          ) AS tup
            |      ) AS tup
            |  ) AS tup
            |""".collapseSpace
      }

      "fully unwrapped and fully re-wrapped - nested" in {
        implicit val parentMeta =
          schemaMeta[GrandParent]("GrandParent", _.par.emb.name -> "theName", _.par.name -> "theParentName")
        val q = quote {
          query[GrandParent]
            .map(g => (g.id, g.par))
            .nested
            .map(p => (p._1, p._2.id, p._2.name, p._2.emb))
            .nested
            .map(tup => (tup._1, tup._2, tup._3, tup._4.id, tup._4.name))
            .nested
            .map(tup => (tup._1, tup._2, tup._3, tup._4, tup._5))
            .nested
            .map(tup => (tup._1, tup._2, tup._3, Emb(tup._4, tup._5)))
            .nested
            .map(tup => (tup._1, Parent(tup._2, tup._3, tup._4)))
            .nested
            .map(tup => GrandParent(tup._1, tup._2))

        }
        println(ctx.run(q).string(true))
        ctx.run(q).string(true).collapseSpace mustEqual
          """|SELECT
             |  tup._1 AS id,
             |  tup._2id AS id,
             |  tup._2name AS name,
             |  tup._2embid AS id,
             |  tup._2embname AS name
             |FROM
             |  (
             |    SELECT
             |      tup._1,
             |      tup._2 AS _2id,
             |      tup._3 AS _2name,
             |      tup._4id AS _2embid,
             |      tup._4name AS _2embname
             |    FROM
             |      (
             |        SELECT
             |          tup._1,
             |          tup._2,
             |          tup._3,
             |          tup._4 AS _4id,
             |          tup._5 AS _4name
             |        FROM
             |          (
             |            SELECT
             |              tup._1,
             |              tup._2,
             |              tup._3,
             |              tup._4,
             |              tup._5
             |            FROM
             |              (
             |                SELECT
             |                  tup._1,
             |                  tup._2,
             |                  tup._3,
             |                  tup._4id AS _4,
             |                  tup._4theName AS _5
             |                FROM
             |                  (
             |                    SELECT
             |                      p._1,
             |                      p._2id AS _2,
             |                      p._2theParentName AS _3,
             |                      p._2embid AS _4id,
             |                      p._2embtheName AS _4theName
             |                    FROM
             |                      (
             |                        SELECT
             |                          g.id AS _1,
             |                          g.id AS _2id,
             |                          g.theParentName AS _2theParentName,
             |                          g.id AS _2embid,
             |                          g.theName AS _2embtheName
             |                        FROM
             |                          GrandParent g
             |                      ) AS p
             |                  ) AS tup
             |              ) AS tup
             |          ) AS tup
             |      ) AS tup
             |  ) AS tup
             |""".collapseSpace
      }

      "fully unwrapped and fully re-wrapped - nested and distinct" in {
        implicit val parentMeta =
          schemaMeta[GrandParent]("GrandParent", _.par.emb.name -> "theName", _.par.name -> "theParentName")
        val q = quote {
          query[GrandParent]
            .map(g => (g.id, g.par /*_2id, _2theParentName, _2embid, _2embtheName*/ ))
            .nested
            .map(p => (p._1, p._2.id, p._2.name, p._2.emb /*_4id, _4theName*/ ))
            .distinct
            .map(tup => (tup._1, tup._2, tup._3, tup._4.id, tup._4.name))
            .nested
            .map(tup => (tup._1, tup._2, tup._3, tup._4, tup._5))
            .distinct
            .map(tup => (tup._1, tup._2, tup._3, Emb(tup._4, tup._5) /*_4id, _4name*/ ))
            .nested
            .map(tup => (tup._1, Parent(tup._2, tup._3, tup._4) /* _2id, _2name, _2embid, _2embname */ ))
            .distinct
            .map(tup => GrandParent(tup._1, tup._2))

        }
        println(ctx.run(q).string(true))
        ctx.run(q).string(true).collapseSpace mustEqual
          """|SELECT
             |  tup._1 AS id,
             |  tup._2id AS id,
             |  tup._2name AS name,
             |  tup._2embid AS id,
             |  tup._2embname AS name
             |FROM
             |  (
             |    SELECT
             |      DISTINCT tup._1,
             |      tup._2 AS _2id,
             |      tup._3 AS _2name,
             |      tup._4id AS _2embid,
             |      tup._4name AS _2embname
             |    FROM
             |      (
             |        SELECT
             |          tup._1,
             |          tup._2,
             |          tup._3,
             |          tup._4 AS _4id,
             |          tup._5 AS _4name
             |        FROM
             |          (
             |            SELECT
             |              DISTINCT tup._1,
             |              tup._2,
             |              tup._3,
             |              tup._4,
             |              tup._5
             |            FROM
             |              (
             |                SELECT
             |                  tup._1,
             |                  tup._2,
             |                  tup._3,
             |                  tup._4id AS _4,
             |                  tup._4theName AS _5
             |                FROM
             |                  (
             |                    SELECT
             |                      DISTINCT p._1,
             |                      p._2id AS _2,
             |                      p._2theParentName AS _3,
             |                      p._2embid AS _4id,
             |                      p._2embtheName AS _4theName
             |                    FROM
             |                      (
             |                        SELECT
             |                          g.id AS _1,
             |                          g.id AS _2id,
             |                          g.theParentName AS _2theParentName,
             |                          g.id AS _2embid,
             |                          g.theName AS _2embtheName
             |                        FROM
             |                          GrandParent g
             |                      ) AS p
             |                  ) AS tup
             |              ) AS tup
             |          ) AS tup
             |      ) AS tup
             |  ) AS tup
          """.collapseSpace
      }
    }

    "adversarial tests" - {
      "should correctly rename the right property when multiple nesting layers have the same one" in {
        case class Emb(name: String, id: Int)
        case class Parent(name: String, emb1: Emb, emb2: Emb)
        case class GrandParent(name: String, par: Parent)

        val norm = quote(query[Emb])
        val mod  = quote(querySchema[Emb]("CustomEmb", _.name -> "theName"))
        val q = quote {
          norm
            .join(mod)
            .on((norm, mod) => norm.name == mod.name)
            .map(joined => Parent("Joe", joined._1, joined._2))
            .distinct
        }

        ctx.run(q).string(true).collapseSpace mustEqual
          """
            |SELECT
            |  DISTINCT 'Joe' AS name,
            |  norm.name,
            |  norm.id,
            |  mod.theName,
            |  mod.id
            |FROM
            |  Emb norm
            |  INNER JOIN CustomEmb mod ON norm.name = mod.theName
            |""".collapseSpace
      }

      "entities re-ordered in a subschema should have correct naming" in {
        case class Ent(name: String)
        case class Foo(fame: String)
        case class Bar(bame: String)

        implicit val entSchema = schemaMeta[Ent]("TheEnt", _.name -> "theName")

        val q = quote {
          query[Foo]
            .join(query[Ent])
            .on((f, e) => f.fame == e.name) // (Foo, Ent)
            .distinct
            .join(query[Bar])
            .on((fe, b) => (fe._1.fame == b.bame)) // ((Foo, Ent), Bar)
            .distinct
            .map(feb => (feb._1._2, feb._2)) // feb: ((Foo, Ent), Bar)
            .distinct
            .map(eb => (eb._1.name, eb._2.bame)) // eb: (Ent, Bar)
        }
        ctx.run(q).string(true).collapseSpace mustEqual
          """
            |SELECT
            |  feb._1theName AS _1,
            |  feb._2bame AS _2
            |FROM
            |  (
            |    SELECT
            |      DISTINCT feb._1_2theName AS _1theName,
            |      feb._2bame
            |    FROM
            |      (
            |        SELECT
            |          DISTINCT fe._1fame AS _1_1fame,
            |          fe._2theName AS _1_2theName,
            |          b.bame AS _2bame
            |        FROM
            |          (
            |            SELECT
            |              DISTINCT f.fame AS _1fame,
            |              e.theName AS _2theName
            |            FROM
            |              Foo f
            |              INNER JOIN TheEnt e ON f.fame = e.theName
            |          ) AS fe
            |          INNER JOIN Bar b ON fe._1fame = b.bame
            |      ) AS feb
            |  ) AS feb
            |""".collapseSpace
      }

      "entities swapped in a subschema should have correct naming" in { // This was a big issue with previous implementation of RenameProperties. See #1618 for more detail.
        case class Ent(name: String)
        case class WrongEnt(name: String)
        case class Bar(bame: String)

        implicit val entSchema = schemaMeta[Ent]("TheEnt", _.name -> "theName") // helloooooooooo

        val q = quote {
          query[WrongEnt]
            .join(query[Ent])
            .on((f, e) => f.name == e.name) // (WrongEnt, Ent)
            .distinct
            .join(query[Bar])
            .on((we, b) => (we._1.name == b.bame)) // ((WrongEnt, Ent), Bar)
            .distinct
            .map(web => ((web._1._2, web._1._1), web._2)) // web: ((WrongEnt, Ent), Bar) -> ((Ent, WrongEnt), Bar)
            .distinct
            .map(ewb => (ewb._1._2.name, ewb._1._1.name, ewb._2.bame)) // ewb: ((WrongEnt, Ent), Bar)
        }
        ctx.run(q).string(true).collapseSpace mustEqual
          """
            |SELECT
            |  web._1_2name AS _1,
            |  web._1_1theName AS _2,
            |  web._2bame AS _3
            |FROM
            |  (
            |    SELECT
            |      DISTINCT web._1_2theName AS _1_1theName,
            |      web._1_1name AS _1_2name,
            |      web._2bame
            |    FROM
            |      (
            |        SELECT
            |          DISTINCT we._1name AS _1_1name,
            |          we._2theName AS _1_2theName,
            |          b.bame AS _2bame
            |        FROM
            |          (
            |            SELECT
            |              DISTINCT f.name AS _1name,
            |              e.theName AS _2theName
            |            FROM
            |              WrongEnt f
            |              INNER JOIN TheEnt e ON f.name = e.theName
            |          ) AS we
            |          INNER JOIN Bar b ON we._1name = b.bame
            |      ) AS web
            |  ) AS web
            |""".collapseSpace
      }
    }

    "query with single embedded element" - {
      case class Emb(a: Int, b: Int)
      case class Parent(id: Int, emb1: Emb)
      case class Parent2(emb1: Emb, id: Int)

      "should not use override from parent schema level - single" in {
        implicit val parentSchema = schemaMeta[Parent]("ParentTable", _.emb1.a -> "field_a")

        val q = quote {
          query[Emb].map(e => Parent(1, e)).distinct.map(p => p.emb1.a)
        }
        ctx.run(q).string mustEqual "SELECT e.emb1a AS a FROM (SELECT DISTINCT 1 AS id, e.a AS emb1a, e.b AS emb1b FROM Emb e) AS e"
      }

      "should not use override from parent schema level - nested" in {
        implicit val parentSchema = schemaMeta[Parent]("ParentTable", _.emb1.a -> "field_emb_a", _.emb1 -> "field_emb")

        val q = quote {
          query[Emb].map(e => Parent(1, e)).distinct.map(p => p.emb1)
        }
        ctx.run(q).string mustEqual "SELECT e.emb1a AS a, e.emb1b AS b FROM (SELECT DISTINCT 1 AS id, e.a AS emb1a, e.b AS emb1b FROM Emb e) AS e"
      }

      "with a schema" - {
        implicit val embSchema = schemaMeta[Emb]("EmbTable", _.a -> "field_a")

        "should use override from child schema level - nested" in {
          val q = quote {
            query[Emb].map(e => (1, e)).distinct.map(p => p._2)
          }
          ctx.run(q).string mustEqual "SELECT e._2field_a AS field_a, e._2b AS b FROM (SELECT DISTINCT 1 AS _1, e.field_a AS _2field_a, e.b AS _2b FROM EmbTable e) AS e"
        }

        "should use override from child schema level - nested - reversed" in {
          val q = quote {
            query[Emb].map(e => (e, 1)).distinct.map(p => p._1)
          }
          ctx.run(q).string mustEqual "SELECT e._1field_a AS field_a, e._1b AS b FROM (SELECT DISTINCT e.field_a AS _1field_a, e.b AS _1b, 1 AS _2 FROM EmbTable e) AS e"
        }

        "should use override from child schema level - nested - case class" in {
          val q = quote {
            query[Emb].map(e => Parent(1, e)).distinct.map(p => p.emb1)
          }
          ctx.run(q).string mustEqual "SELECT e.emb1field_a AS field_a, e.emb1b AS b FROM (SELECT DISTINCT 1 AS id, e.field_a AS emb1field_a, e.b AS emb1b FROM EmbTable e) AS e"
        }

        "should use override from child schema level - nested - case class - reversed" in {
          val q = quote {
            query[Emb].map(e => Parent2(e, 1)).distinct.map(p => p.emb1)
          }
          ctx.run(q).string mustEqual "SELECT e.emb1field_a AS field_a, e.emb1b AS b FROM (SELECT DISTINCT e.field_a AS emb1field_a, e.b AS emb1b, 1 AS id FROM EmbTable e) AS e"
        }
      }
    }

    "query with multiple embedded elements with same names" - {
      case class Emb(name: String, id: Int)
      case class Parent(name: String, emb1: Emb, emb2: Emb)
      case class GrandParent(name: String, par: Parent)

      case class One(name: String, id: Int)
      case class Two(name: String, id: Int)
      case class Dual(one: One, two: Two)

      // Try parent and embedded children with same name, schema on parent
      "schema on parent should not override children" in {
        implicit val parentSchema = schemaMeta[Parent]("ParentEnt", _.name -> "theName")
        val q = quote {
          query[Emb].map(e => Parent("Joe", e, e)).distinct.map(p => p.emb1)
        }
        ctx.run(q).string mustEqual "SELECT e.emb1name AS name, e.emb1id AS id FROM (SELECT DISTINCT 'Joe' AS name, e.name AS emb1name, e.id AS emb1id, e.name AS emb2name, e.id AS emb2id FROM Emb e) AS e"
      }

      "schema on parent should not override children - from grandparent ad-hoc cc" in {
        implicit val parentSchema = schemaMeta[Parent]("ParentEnt", _.name -> "theName")
        val q = quote {
          query[Parent].map(p => GrandParent("GJoe", p)).distinct.map(p => (p.par.emb1, p.par.name))
        }
        ctx.run(q).string mustEqual "SELECT p.paremb1name AS name, p.paremb1id AS id, p.partheName AS _2 FROM (SELECT DISTINCT 'GJoe' AS name, p.theName AS partheName, p.name AS paremb1name, p.id AS paremb1id, p.name AS paremb2name, p.id AS paremb2id FROM ParentEnt p) AS p"
      }

      // Schema on child should propagate to children
      "schema on children should behave correctly when inside parent" in {
        implicit val childSchema = schemaMeta[Emb]("ChildEnt", _.name -> "theName")
        val q = quote {
          query[Emb].map(e => Parent("Joe", e, e)).distinct.map(p => p.emb1)
        }
        ctx.run(q).string mustEqual "SELECT e.emb1theName AS theName, e.emb1id AS id FROM (SELECT DISTINCT 'Joe' AS name, e.theName AS emb1theName, e.id AS emb1id, e.theName AS emb2theName, e.id AS emb2id FROM ChildEnt e) AS e"
      }

      // Try parent and embedded children with same name, schema on one of children
      "schema on one of children should not override the other child or the parent" in {
        val norms = quote(query[Emb])
        val mods  = quote(querySchema[Emb]("CustomEmb", _.name -> "theName"))
        val q = quote {
          norms
            .join(mods)
            .on((norm, mod) => norm.name == mod.name)
            .map(joined => Parent("Joe", joined._1, joined._2))
            .distinct
        }
        ctx.run(q).string(true).collapseSpace mustEqual
          """
            |SELECT
            |  DISTINCT 'Joe' AS name,
            |  norm.name,
            |  norm.id,
            |  mod.theName,
            |  mod.id
            |FROM
            |  Emb norm
            |  INNER JOIN CustomEmb mod ON norm.name = mod.theName
            |""".collapseSpace
      }

      // Try parent and embedded children with same name, schema on the other child
      "schema on the other one children should not override the other child or the parent" in {
        val norms = quote(query[Emb])
        val mods  = quote(querySchema[Emb]("CustomEmb", _.name -> "theName"))
        val q = quote {
          mods
            .join(norms)
            .on((mod, norm) => norm.name == mod.name)
            .map(joined => Parent("Joe", joined._1, joined._2))
            .distinct
        }
        ctx.run(q).string(true).collapseSpace mustEqual
          """
            |SELECT
            |  DISTINCT 'Joe' AS name,
            |  mod.theName,
            |  mod.id,
            |  norm.name,
            |  norm.id
            |FROM
            |  CustomEmb mod
            |  INNER JOIN Emb norm ON norm.name = mod.theName
            |""".collapseSpace
      }

      // Try parent and embedded children with same name, schema on both of children - same schema
      "schema on both of the children can be the same" in {
        val norms = quote(querySchema[Emb]("CustomEmb", _.name -> "theName"))
        val mods  = quote(querySchema[Emb]("CustomEmb", _.name -> "theName"))
        val q = quote {
          mods
            .join(norms)
            .on((mod, norm) => norm.name == mod.name)
            .map(joined => Parent("Joe", joined._1, joined._2))
            .distinct
        }
        ctx.run(q).string(true).collapseSpace mustEqual
          """
            |SELECT
            |  DISTINCT 'Joe' AS name,
            |  mod.theName,
            |  mod.id,
            |  norm.theName,
            |  norm.id
            |FROM
            |  CustomEmb mod
            |  INNER JOIN CustomEmb norm ON norm.theName = mod.theName
            |""".collapseSpace
      }

      // Try parent and embedded children with same name, schema on both of children - different schemas
      "schema on both of the children can be different" in {
        val norms = quote(querySchema[Emb]("CustomEmb", _.name -> "theFirstName"))
        val mods  = quote(querySchema[Emb]("CustomEmb", _.name -> "theSecondName"))
        val q = quote {
          mods
            .join(norms)
            .on((mod, norm) => norm.name == mod.name)
            .map(joined => Parent("Joe", joined._1, joined._2))
            .distinct
        }
        ctx.run(q).string(true).collapseSpace mustEqual
          """
            |SELECT
            |  DISTINCT 'Joe' AS name,
            |  mod.theSecondName,
            |  mod.id,
            |  norm.theFirstName,
            |  norm.id
            |FROM
            |  CustomEmb mod
            |  INNER JOIN CustomEmb norm ON norm.theFirstName = mod.theSecondName
            |""".collapseSpace
      }
    }
  }
}
