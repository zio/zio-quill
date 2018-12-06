package io.getquill.context.sql.norm

import io.getquill.{ MirrorSqlDialect, SnakeCase, Spec, SqlMirrorContext }
import io.getquill.context.sql.testContext

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
      "SELECT x._1s, x._1i, x._1l, x._1o, x._2s, x._2i, x._2l, x._2o FROM (SELECT DISTINCT a.l AS _1l, a.o AS _1o, a.s AS _1s, a.i AS _1i, b.o AS _2o, b.s AS _2s, b.i AS _2i, b.l AS _2l FROM TestEntity a FULL JOIN TestEntity2 b ON a.i = b.i) AS x"
  }

  "handles column alias conflict" in {
    import testContext._
    val q = quote {
      qr1.join(qr2).on((a, b) => a.i == b.i).nested.map {
        case (a, b) => (a.i, b.i)
      }
    }
    testContext.run(q.dynamic).string mustEqual
      "SELECT x02._1i, x02._2i FROM (SELECT a.i AS _1i, b.i AS _2i FROM TestEntity a INNER JOIN TestEntity2 b ON a.i = b.i) AS x02"
  }
}
