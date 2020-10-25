package io.getquill.context.sql.norm

import io.getquill.{ MirrorSqlDialect, Query, SnakeCase, Spec, SqlMirrorContext }
import io.getquill.context.sql.{ testContext, testContextUpperEscapeColumn }
import io.getquill.context.sql.util.StringOps._

class ExpandNestedQueriesSpec extends Spec {

  "keeps the initial table alias" in {
    import testContext._
    val q = quote {
      (for {
        a <- qr1
        b <- qr2
      } yield b).nested
    }

    testContext.run(q).string mustEqual
      "SELECT x.s, x.i, x.l, x.o FROM (SELECT x.s, x.i, x.l, x.o FROM TestEntity a, TestEntity2 x) AS x"
  }

  "multi-nests correctly" in {
    import testContext._
    case class MyPerson(first: String, last: String, age: Int)
    val q = quote {
      query[MyPerson].nested.nested
    }
    testContext.run(q).string mustEqual
      "SELECT x.first, x.last, x.age FROM (SELECT x.first, x.last, x.age FROM (SELECT x.first, x.last, x.age FROM MyPerson x) AS x) AS x"
  }

  "multi-nests correctly with exclusions" in {
    import testContext._
    case class MyPerson(first: String, last: String, age: Int)
    val q = quote {
      query[MyPerson].nested.nested.map(p => (p.first, p.last))
    }
    testContext.run(q).string mustEqual
      "SELECT p.first, p.last FROM (SELECT x.first, x.last FROM (SELECT x.first, x.last FROM MyPerson x) AS x) AS p"
  }

  "preserves order of selection" in {
    import testContext._
    val q = quote {
      query[TestEntity]
        .join(query[TestEntity2])
        .on { case (one, two) => one.i == two.i }
        .filter(_._1.s == "foo")
        .map(_._2)
        .map(e => (infix"DISTINCT ON (${e.s}) ${e.s}".as[String], e.i))
        .filter(_._2 == 123)
    }
    testContext.run(q).string mustEqual
      "SELECT x1._1, x1._2 FROM (SELECT DISTINCT ON (x11.s) x11.s AS _1, x11.i AS _2 FROM TestEntity x01 INNER JOIN TestEntity2 x11 ON x01.i = x11.i WHERE x01.s = 'foo') AS x1 WHERE x1._2 = 123"
  }

  "partial select" in {
    import testContext._
    val q = quote {
      (for {
        a <- qr1
        b <- qr2
      } yield (b.i, a.i)).nested
    }
    testContext.run(q).string mustEqual
      "SELECT x._1, x._2 FROM (SELECT b.i AS _1, a.i AS _2 FROM TestEntity a, TestEntity2 b) AS x"
  }

  "tokenize property" in {
    object testContext extends SqlMirrorContext(MirrorSqlDialect, SnakeCase)
    import testContext._

    case class Entity(camelCase: String)

    testContext.run(
      query[Entity]
        .map(e => (e, 1))
        .nested
    ).string mustEqual
      "SELECT e.camelCase, 1 FROM (SELECT x.camel_case AS camelCase FROM entity x) AS e"
  }

  "expands nested tuple select" in {
    import testContext._
    val q = quote {
      qr1.groupBy(s => (s.i, s.s)).map {
        case (group, items) =>
          (group, items.size)
      }
    }
    testContext.run(q).string mustEqual
      "SELECT s.i, s.s, COUNT(*) FROM TestEntity s GROUP BY s.i, s.s"
  }

  "expands nested distinct query" in {
    import testContext._
    val q = quote {
      qr1.fullJoin(qr2).on((a, b) => a.i == b.i).distinct
    }
    testContext.run(q).string mustEqual
      "SELECT x._1s, x._1i, x._1l, x._1o, x._1b, x._2s, x._2i, x._2l, x._2o FROM (SELECT DISTINCT a.s AS _1s, a.i AS _1i, a.l AS _1l, a.o AS _1o, a.b AS _1b, b.s AS _2s, b.i AS _2i, b.l AS _2l, b.o AS _2o FROM TestEntity a FULL JOIN TestEntity2 b ON a.i = b.i) AS x"
  }

  "handles column alias conflict" in {
    import testContext._
    val q = quote {
      qr1.join(qr2).on((a, b) => a.i == b.i).nested.map {
        case (a, b) => (a.i, b.i)
      }
    }
    println(testContext.run(q).string(true))
    testContext.run(q).string mustEqual
      "SELECT x03._1i, x03._2i FROM (SELECT a.i AS _1i, b.i AS _2i FROM TestEntity a INNER JOIN TestEntity2 b ON a.i = b.i) AS x03"
  }

  "expands nested mapped entity correctly" in {
    import testContext._

    case class TestEntity(s: String, i: Int, l: Long, o: Option[Int]) extends Embedded
    case class Dual(ta: TestEntity, tb: TestEntity)

    val qr1 = quote {
      query[TestEntity]
    }

    val q = quote {
      qr1.join(qr1).on((a, b) => a.i == b.i).nested.map(both => both match { case (a, b) => Dual(a, b) }).nested
    }
    testContext.run(q).string(true).collapseSpace mustEqual
      """SELECT
        |  both._1s,
        |  both._1i,
        |  both._1l,
        |  both._1o,
        |  both._2s,
        |  both._2i,
        |  both._2l,
        |  both._2o
        |FROM
        |  (
        |    SELECT
        |      x._1s,
        |      x._1i,
        |      x._1l,
        |      x._1o,
        |      x._2s,
        |      x._2i,
        |      x._2l,
        |      x._2o
        |    FROM
        |      (
        |        SELECT
        |          a.s AS _1s,
        |          a.i AS _1i,
        |          a.l AS _1l,
        |          a.o AS _1o,
        |          b.s AS _2s,
        |          b.i AS _2i,
        |          b.l AS _2l,
        |          b.o AS _2o
        |        FROM
        |          TestEntity a
        |          INNER JOIN TestEntity b ON a.i = b.i
        |      ) AS x
        |  ) AS both
        |""".collapseSpace
  }

  "nested with distinct" - {
    val ctx = testContext
    import ctx._

    "embedded, distinct entity in sub-tuple" in {
      case class Parent(id: Int, emb: Emb)
      case class Emb(name: String, id: Int) extends Embedded

      val q = quote {
        query[Parent].map(p => (p.emb, 1)).distinct.map(e => (e._1.name, e._1.id))
      }

      ctx.run(q).string mustEqual "SELECT p._1name, p._1id FROM (SELECT DISTINCT p.name AS _1name, p.id AS _1id, 1 AS _2 FROM Parent p) AS p"
    }

    "embedded, distinct entity in case class" in {
      case class Parent(id: Int, emb: Emb)
      case class Emb(name: String, id: Int) extends Embedded
      case class SuperParent(emb: Emb, id: Int)

      val q = quote {
        query[Parent].map(p => SuperParent(p.emb, 1)).distinct.map(e => (e.emb.name, e.emb.id))
      }

      ctx.run(q).string mustEqual "SELECT p.embname, p.embid FROM (SELECT DISTINCT p.name AS embname, p.id AS embid, 1 AS id FROM Parent p) AS p"
    }

    "can be propagated across nested query with naming intact" in {
      case class Parent(id: Int, emb: Emb)
      case class Emb(name: String, id: Int) extends Embedded

      val q = quote {
        query[Parent].map(p => p.emb).nested.map(e => (e.name, e.id))
      }
      ctx.run(q).string mustEqual "SELECT p.embname, p.embid FROM (SELECT x.name AS embname, x.id AS embid FROM Parent x) AS p"
    }

    "can be propagated across distinct query with naming intact" in {
      case class Parent(id: Int, emb: Emb)
      case class Emb(name: String, id: Int) extends Embedded

      val q = quote {
        query[Parent].map(p => p.emb).distinct.map(e => (e.name, e.id))
      }
      ctx.run(q).string mustEqual "SELECT e.name, e.id FROM (SELECT DISTINCT p.name, p.id FROM Parent p) AS e"
    }

    "can be propagated across distinct query with naming intact - double distinct" in {
      case class Parent(id: Int, emb: Emb)
      case class Emb(name: String, id: Int) extends Embedded

      val q = quote {
        query[Parent].map(p => p.emb).distinct.map(e => (e.name, e.id)).distinct
      }
      ctx.run(q).string mustEqual "SELECT DISTINCT e.name, e.id FROM (SELECT DISTINCT p.name, p.id FROM Parent p) AS e"
    }

    "can be propagated across distinct query with naming intact then re-wrapped into the parent" in {
      case class Parent(id: Int, emb: Emb)
      case class Emb(name: String, id: Int) extends Embedded

      val q = quote {
        query[Parent].map(p => p.emb).distinct.map(e => (e.name, e.id)).distinct.map(tup => Emb(tup._1, tup._2)).distinct
      }
      ctx.run(q).string.collapseSpace mustEqual
        """SELECT tup.name, tup.id
          |FROM (SELECT DISTINCT tup._1 AS name, tup._2 AS id
          |      FROM (SELECT DISTINCT e.name AS _1, e.id AS _2
          |            FROM (SELECT DISTINCT p.name, p.id FROM Parent p) AS e) AS tup) AS tup
        """.stripMargin.collapseSpace
    }
  }

  "multiple embedding levels" in {
    import testContext._
    case class Emb(id: Int, name: String) extends Embedded
    case class Parent(id: Int, name: String, emb: Emb) extends Embedded
    case class GrandParent(id: Int, par: Parent)

    val q = quote {
      query[GrandParent]
        .map(g => (g.id, g.par)).distinct
        .map(p => (p._1, p._2.id, p._2.name, p._2.emb)).distinct
        .map(tup => (tup._1, tup._2, tup._3, tup._4.id, tup._4.name)).distinct
        .map(tup => (tup._1, tup._2, tup._3, tup._4, tup._5)).distinct
        .map(tup => (tup._1, tup._2, tup._3, Emb(tup._4, tup._5))).distinct
        .map(tup => (tup._1, Parent(tup._2, tup._3, tup._4))).distinct
        .map(tup => GrandParent(tup._1, tup._2)).distinct
    }

    val str = testContext.run(q).string(true)
    println(str)
    testContext.run(q).string.collapseSpace mustEqual
      """SELECT tup.id, tup.parid, tup.parname, tup.parembid, tup.parembname
        |FROM (SELECT DISTINCT tup._1        AS id,
        |                      tup._2id      AS parid,
        |                      tup._2name    AS parname,
        |                      tup._2embid   AS parembid,
        |                      tup._2embname AS parembname
        |      FROM (SELECT DISTINCT tup._1,
        |                            tup._2     AS _2id,
        |                            tup._3     AS _2name,
        |                            tup._4id   AS _2embid,
        |                            tup._4name AS _2embname
        |            FROM (SELECT DISTINCT tup._1,
        |                                  tup._2,
        |                                  tup._3,
        |                                  tup._4 AS _4id,
        |                                  tup._5 AS _4name
        |                  FROM (SELECT DISTINCT tup._1,
        |                                        tup._2,
        |                                        tup._3,
        |                                        tup._4id   AS _4,
        |                                        tup._4name AS _5
        |                        FROM (SELECT DISTINCT p._1,
        |                                              p._2id      AS _2,
        |                                              p._2name    AS _3,
        |                                              p._2embid   AS _4id,
        |                                              p._2embname AS _4name
        |                              FROM (SELECT DISTINCT g.id   AS _1,
        |                                                    g.id   AS _2id,
        |                                                    g.name AS _2name,
        |                                                    g.id   AS _2embid,
        |                                                    g.name AS _2embname
        |                                    FROM GrandParent g) AS p) AS tup) AS tup) AS tup) AS tup) AS tup
      """.collapseSpace
  }

  "multiple embedding levels - another example" in {
    import testContext._
    case class Sim(sid: Int) extends Embedded
    case class Mam(mid: Int, sim: Sim) extends Embedded
    case class Bim(bid: Int, mam: Mam)

    val q = quote {
      query[Bim]
        .map(g => (g.bid, g.mam)).distinct
        .map(p => (p._1, p._2.mid, p._2.sim)).distinct
        .map(tup => (tup._1, tup._2, tup._3)).distinct
        .map(tup => (tup._1, tup._2, tup._3.sid)).distinct
        .map(tup => (tup._1, tup._2, Sim(tup._3))).distinct
        .map(tup => (tup._1, Mam(tup._2, tup._3))).distinct
        .map(tup => Bim(tup._1, tup._2)).distinct
    }
    testContext.run(q).string(true).collapseSpace mustEqual
      """
        |SELECT
        |  tup.bid,
        |  tup.mammid,
        |  tup.mamsimsid
        |FROM
        |  (
        |    SELECT
        |      DISTINCT tup._1 AS bid,
        |      tup._2mid AS mammid,
        |      tup._2simsid AS mamsimsid
        |    FROM
        |      (
        |        SELECT
        |          DISTINCT tup._1,
        |          tup._2 AS _2mid,
        |          tup._3sid AS _2simsid
        |        FROM
        |          (
        |            SELECT
        |              DISTINCT tup._1,
        |              tup._2,
        |              tup._3 AS _3sid
        |            FROM
        |              (
        |                SELECT
        |                  DISTINCT tup._1,
        |                  tup._2,
        |                  tup._3sid AS _3
        |                FROM
        |                  (
        |                    SELECT
        |                      DISTINCT p._1,
        |                      p._2mid AS _2,
        |                      p._2simsid AS _3sid
        |                    FROM
        |                      (
        |                        SELECT
        |                          DISTINCT g.bid AS _1,
        |                          g.mid AS _2mid,
        |                          g.sid AS _2simsid
        |                        FROM
        |                          Bim g
        |                      ) AS p
        |                  ) AS tup
        |              ) AS tup
        |          ) AS tup
        |      ) AS tup
        |  ) AS tup
        |""".collapseSpace
  }

  "multiple embedding levels - another example - with rename" in {
    import testContext._
    case class Sim(sid: Int) extends Embedded
    case class Mam(mid: Int, sim: Sim) extends Embedded
    case class Bim(bid: Int, mam: Mam)

    implicit val bimSchemaMeta = schemaMeta[Bim]("theBim", _.bid -> "theBid", _.mam.sim.sid -> "theSid")

    val q = quote {
      query[Bim]
        .map(g => (g.bid, g.mam)).distinct
        .map(p => (p._1, p._2.mid, p._2.sim)).distinct
        .map(tup => (tup._1, tup._2, tup._3)).distinct
        .map(tup => (tup._1, tup._2, tup._3.sid)).distinct
        .map(tup => (tup._1, tup._2, Sim(tup._3))).distinct
        .map(tup => (tup._1, Mam(tup._2, tup._3))).distinct
        .map(tup => Bim(tup._1, tup._2)).distinct
    }
    println(testContext.run(q).string(true))
    testContext.run(q).string(true).collapseSpace mustEqual
      """
        |SELECT
        |  tup.bid,
        |  tup.mammid,
        |  tup.mamsimsid
        |FROM
        |  (
        |    SELECT
        |      DISTINCT tup._1 AS bid,
        |      tup._2mid AS mammid,
        |      tup._2simsid AS mamsimsid
        |    FROM
        |      (
        |        SELECT
        |          DISTINCT tup._1,
        |          tup._2 AS _2mid,
        |          tup._3sid AS _2simsid
        |        FROM
        |          (
        |            SELECT
        |              DISTINCT tup._1,
        |              tup._2,
        |              tup._3 AS _3sid
        |            FROM
        |              (
        |                SELECT
        |                  DISTINCT tup._1,
        |                  tup._2,
        |                  tup._3theSid AS _3
        |                FROM
        |                  (
        |                    SELECT
        |                      DISTINCT p._1,
        |                      p._2mid AS _2,
        |                      p._2simtheSid AS _3theSid
        |                    FROM
        |                      (
        |                        SELECT
        |                          DISTINCT g.theBid AS _1,
        |                          g.mid AS _2mid,
        |                          g.theSid AS _2simtheSid
        |                        FROM
        |                          theBim g
        |                      ) AS p
        |                  ) AS tup
        |              ) AS tup
        |          ) AS tup
        |      ) AS tup
        |  ) AS tup
        |""".collapseSpace
  }

  "multiple embedding levels - another example - with rename - with escape column" in {
    val ctx = testContextUpperEscapeColumn
    import ctx._
    case class Sim(sid: Int) extends Embedded
    case class Mam(mid: Int, sim: Sim) extends Embedded
    case class Bim(bid: Int, mam: Mam)

    implicit val bimSchemaMeta = schemaMeta[Bim]("theBim", _.bid -> "theBid", _.mam.sim.sid -> "theSid")

    val q = quote {
      query[Bim]
        .map(g => (g.bid, g.mam)).distinct
        .map(p => (p._1, p._2.mid, p._2.sim)).distinct
        .map(tup => (tup._1, tup._2, tup._3)).distinct
        .map(tup => (tup._1, tup._2, tup._3.sid)).distinct
        .map(tup => (tup._1, tup._2, Sim(tup._3))).distinct
        .map(tup => (tup._1, Mam(tup._2, tup._3))).distinct
        .map(tup => Bim(tup._1, tup._2)).distinct
    }
    ctx.run(q).string(true).collapseSpace mustEqual
      """
        |SELECT
        |  tup.bid,
        |  tup.mammid,
        |  tup.mamsimsid
        |FROM
        |  (
        |    SELECT
        |      DISTINCT tup._1 AS bid,
        |      tup._2mid AS mammid,
        |      tup._2simsid AS mamsimsid
        |    FROM
        |      (
        |        SELECT
        |          DISTINCT tup._1,
        |          tup._2 AS _2mid,
        |          tup._3sid AS _2simsid
        |        FROM
        |          (
        |            SELECT
        |              DISTINCT tup._1,
        |              tup._2,
        |              tup._3 AS _3sid
        |            FROM
        |              (
        |                SELECT
        |                  DISTINCT tup._1,
        |                  tup._2,
        |                  tup._3theSid AS _3
        |                FROM
        |                  (
        |                    SELECT
        |                      DISTINCT p._1,
        |                      p._2mid AS _2,
        |                      p._2simtheSid AS _3theSid
        |                    FROM
        |                      (
        |                        SELECT
        |                          DISTINCT g.theBid AS _1,
        |                          g."MID" AS _2mid,
        |                          g.theSid AS _2simtheSid
        |                        FROM
        |                          theBim g
        |                      ) AS p
        |                  ) AS tup
        |              ) AS tup
        |          ) AS tup
        |      ) AS tup
        |  ) AS tup
        |""".collapseSpace
  }

  "multiple embedding levels - another example - with rename - with escape column - with groupby" in {
    val ctx = testContextUpperEscapeColumn
    import ctx._
    case class Sim(sid: Int) extends Embedded
    case class Mam(mid: Int, sim: Sim) extends Embedded
    case class Bim(bid: Int, mam: Mam)

    implicit val bimSchemaMeta = schemaMeta[Bim]("theBim", _.bid -> "theBid", _.mam.sim.sid -> "theSid")

    /*
    The following Does not work
    val q = quote {
      query[Bim]
        .map(g => (g.bid, g.mam)).distinct //.sortBy(_._2.sim.sid)
        .map(p => (p._1, p._2.mid, p._2.sim)).distinct
        .map(tup => (tup._1, tup._2, tup._3)).filter(n => n._3.sid == 1).distinct
     */

    /*
    Fix by doing this
    val q = quote {
      query[Bim]
        .map(g => (g.bid, g.mam)).distinct //.sortBy(_._2.sim.sid)
        .map(p => (p._1, p._2.mid, p._2.sim)).distinct
        .map(tup => (tup._1, tup._2, tup._3)).nested.filter(n => n._3.sid == 1).distinct
     */

    val q = quote {
      query[Bim]
        .map(g => (g.bid, g.mam)).distinct.sortBy(_._2.sim.sid)
        .map(p => (p._1, p._2.mid, p._2.sim)).distinct
        .map(tup => (tup._1, tup._2, tup._3)).nested.filter(n => n._3.sid == 1).distinct
        .map(tup => (tup._1, tup._2, tup._3.sid)).distinct
        .map(tup => (tup._1, tup._2, Sim(tup._3))).distinct
        .map(tup => (tup._1, Mam(tup._2, tup._3))).distinct
        .map(tup => Bim(tup._1, tup._2)).distinct
    }
    ctx.run(q).string(true).collapseSpace mustEqual
      """
        |SELECT
        |  tup.bid,
        |  tup.mammid,
        |  tup.mamsimsid
        |FROM
        |  (
        |    SELECT
        |      DISTINCT tup._1 AS bid,
        |      tup._2mid AS mammid,
        |      tup._2simsid AS mamsimsid
        |    FROM
        |      (
        |        SELECT
        |          DISTINCT tup._1,
        |          tup._2 AS _2mid,
        |          tup._3sid AS _2simsid
        |        FROM
        |          (
        |            SELECT
        |              DISTINCT tup._1,
        |              tup._2,
        |              tup._3 AS _3sid
        |            FROM
        |              (
        |                SELECT
        |                  DISTINCT tup._1,
        |                  tup._2,
        |                  tup._3theSid AS _3
        |                FROM
        |                  (
        |                    SELECT
        |                      DISTINCT n._1,
        |                      n._2,
        |                      n._3theSid
        |                    FROM
        |                      (
        |                        SELECT
        |                          DISTINCT x10._1,
        |                          x10._2mid AS _2,
        |                          x10._2simtheSid AS _3theSid
        |                        FROM
        |                          (
        |                            SELECT
        |                              DISTINCT g.theBid AS _1,
        |                              g."MID" AS _2mid,
        |                              g.theSid AS _2simtheSid
        |                            FROM
        |                              theBim g
        |                            ORDER BY
        |                              g.theSid ASC NULLS FIRST
        |                          ) AS x10
        |                      ) AS n
        |                    WHERE
        |                      n._3theSid = 1
        |                  ) AS tup
        |              ) AS tup
        |          ) AS tup
        |      ) AS tup
        |  ) AS tup
        |""".collapseSpace // bad
  }

  "multiple embedding levels - without nesting the filter" in {
    val ctx = testContextUpperEscapeColumn
    import ctx._

    case class Sim(sid: Int) extends Embedded
    case class Mam(mid: Int, sim: Sim)

    val q = quote {
      query[Mam]
        .map(tup => (tup.mid, tup.sim)).distinct.sortBy(_._2.sid)
        .map(tup => (tup._1, tup._2)).filter(tup => tup._2.sid == 1).distinct
        .map(tup => (tup._1, tup._2.sid)).distinct
        .map(tup => (tup._1, Sim(tup._2))).distinct
        .map(tup => Mam(tup._1, tup._2)).distinct
    }
    ctx.run(q).string(true).collapseSpace mustEqual
      """
        |SELECT
        |  tup.mid,
        |  tup.simsid
        |FROM
        |  (
        |    SELECT
        |      DISTINCT tup._1 AS mid,
        |      tup._2sid AS simsid
        |    FROM
        |      (
        |        SELECT DISTINCT tup._1,
        |          tup._2 AS _2sid
        |       FROM
        |         (
        |           SELECT
        |             DISTINCT tup._1,
        |             tup._2sid AS _2
        |           FROM
        |             (
        |               SELECT
        |                 DISTINCT x11._1,
        |                 x11._2sid
        |               FROM
        |                 (
        |                  SELECT
        |                    DISTINCT tup."MID" AS _1,
        |                    tup."SID" AS _2sid
        |                  FROM
        |                    Mam tup
        |                  ORDER BY
        |                    tup."SID" ASC NULLS FIRST
        |                ) AS x11
        |              WHERE
        |                x11._2sid = 1
        |            ) AS tup
        |        ) AS tup
        |     ) AS tup
        | ) AS tup
        |""".collapseSpace
  }

  "infixes" - {
    object testContext extends SqlMirrorContext(MirrorSqlDialect, SnakeCase)
    import testContext._

    "should be handled correctly in a regular schema" in {
      case class Person(firstName: String, lastName: String)
      testContext.run(infix"fromSomewhere()".as[Query[Person]]).string mustEqual
        "SELECT x.first_name, x.last_name FROM (fromSomewhere()) AS x"
    }
  }

  "expression subquery" - {
    case class ThePerson(name: String, age: Int, bossId: Int)
    case class TheBoss(bossId: Int, name: String, age: Int)

    object testContext extends SqlMirrorContext(MirrorSqlDialect, SnakeCase)
    import testContext._

    "should be handled correctly in a regular schema" in {
      testContext.run(query[ThePerson].filter(p => query[TheBoss].filter(_.bossId == p.bossId).map(_ => 1).nonEmpty)).string mustEqual
        "SELECT p.name, p.age, p.boss_id FROM the_person p WHERE EXISTS (SELECT 1 FROM the_boss x12 WHERE x12.boss_id = p.boss_id)"
    }
    "should be handled correctly when using a schemameta" in {
      implicit val personSchema = schemaMeta[TheBoss]("theBossMan", _.bossId -> "bossman_id")
      testContext.run(query[ThePerson].filter(p => query[TheBoss].filter(_.bossId == p.bossId).map(_ => 1).nonEmpty)).string mustEqual
        "SELECT p.name, p.age, p.boss_id FROM the_person p WHERE EXISTS (SELECT 1 FROM theBossMan x15 WHERE x15.bossman_id = p.boss_id)"
    }
  }
}
