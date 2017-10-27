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
      "SELECT x.s, x.i, x.l, x.o FROM (SELECT x.s, x.i, x.l, x.o FROM TestEntity a, TestEntity2 x) x"
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
      "SELECT x._1, x._2 FROM (SELECT b.i _1, a.i _2 FROM TestEntity a, TestEntity2 b) x"
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
      "SELECT e.camel_case, 1 FROM (SELECT x.camel_case FROM entity x) e"
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

  "doesn't expand nested distinct query" in {
    import testContext._
    val q = quote {
      (for {
        a <- qr1
        b <- qr2
      } yield {
        (a, b)
      }).distinct
    }
    testContext.run(q).string mustEqual
      "SELECT x.s, x.i, x.l, x.o, x.s, x.i, x.l, x.o FROM (SELECT DISTINCT x.* FROM (SELECT a.*, b.* FROM TestEntity a, TestEntity2 b) x) x"
  }
}
