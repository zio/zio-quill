package io.getquill.context.sql.norm

import io.getquill.{ MirrorSqlDialect, SnakeCase, Spec, SqlMirrorContext }
import io.getquill.context.sql.testContext
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
      "SELECT e.camel_case, 1 FROM (SELECT x.camel_case FROM entity x) AS e"
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
    testContext.run(q.dynamic).string mustEqual
      "SELECT x._1s, x._1i, x._1l, x._1o, x._2s, x._2i, x._2l, x._2o FROM (SELECT DISTINCT a.s AS _1s, a.i AS _1i, a.l AS _1l, a.o AS _1o, b.s AS _2s, b.i AS _2i, b.l AS _2l, b.o AS _2o FROM TestEntity a FULL JOIN TestEntity2 b ON a.i = b.i) AS x"
  }

  "handles column alias conflict" in {
    import testContext._
    val q = quote {
      qr1.join(qr2).on((a, b) => a.i == b.i).nested.map {
        case (a, b) => (a.i, b.i)
      }
    }
    testContext.run(q.dynamic).string mustEqual
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

    testContext.run(q).string mustEqual
      "SELECT both._1s, both._1i, both._1l, both._1o, both._2s, both._2i, both._2l, both._2o FROM (SELECT a.s AS _1s, a.i AS _1i, a.l AS _1l, a.o AS _1o, b.s AS _2s, b.i AS _2i, b.l AS _2l, b.o AS _2o FROM TestEntity a INNER JOIN TestEntity b ON a.i = b.i) AS both"
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

      ctx.run(q).string mustEqual "SELECT p._1name, p._1id FROM (SELECT DISTINCT p.name AS _1name, p.id AS _1id FROM Parent p) AS p"
    }

    "embedded, distinct entity in case class" in {
      case class Parent(id: Int, emb: Emb)
      case class Emb(name: String, id: Int) extends Embedded
      case class SuperParent(emb: Emb, id: Int)

      val q = quote {
        query[Parent].map(p => SuperParent(p.emb, 1)).distinct.map(e => (e.emb.name, e.emb.id))
      }

      ctx.run(q).string mustEqual "SELECT p.embname, p.embid FROM (SELECT DISTINCT p.name AS embname, p.id AS embid FROM Parent p) AS p"
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
      ctx.run(q).string mustEqual "SELECT e.name, e.id FROM (SELECT DISTINCT name AS name, id AS id FROM Parent p) AS e"
    }

    "can be propagated across distinct query with naming intact - double distinct" in {
      case class Parent(id: Int, emb: Emb)
      case class Emb(name: String, id: Int) extends Embedded

      val q = quote {
        query[Parent].map(p => p.emb).distinct.map(e => (e.name, e.id)).distinct
      }
      ctx.run(q).string mustEqual "SELECT DISTINCT e.name, e.id FROM (SELECT DISTINCT name AS name, id AS id FROM Parent p) AS e"
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
          |            FROM (SELECT DISTINCT name AS name, id AS id FROM Parent p) AS e) AS tup) AS tup
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
}
