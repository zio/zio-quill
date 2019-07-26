package io.getquill.context

import io.getquill.SqlMirrorContext
import io.getquill.MirrorSqlDialect
import io.getquill.TestEntities
import io.getquill.Literal
import io.getquill.Spec

class InfixSpec extends Spec {

  "queries with infix should" - {

    val ctx = new SqlMirrorContext(MirrorSqlDialect, Literal) with TestEntities
    import ctx._

    case class Data(id: Int)
    case class TwoValue(id: Int, value: Int)

    "preserve nesting where needed" in {
      val q = quote {
        query[Data].map(e => TwoValue(e.id, infix"RAND()".as[Int])).filter(r => r.value > 10)
      }
      ctx.run(q).string mustEqual "SELECT e.id, e.value FROM (SELECT e.id AS id, RAND() AS value FROM Data e) AS e WHERE e.value > 10"
    }

    "collapse nesting where not needed" in {
      val q = quote {
        query[Data].map(e => TwoValue(e.id, infix"SOMETHINGPURE()".pure.as[Int])).filter(r => r.value > 10)
      }
      ctx.run(q).string mustEqual "SELECT e.id, SOMETHINGPURE() FROM Data e WHERE SOMETHINGPURE() > 10"
    }

    "preserve nesting with single value" in {
      val q = quote {
        query[Data].map(e => infix"RAND()".as[Int]).filter(r => r > 10).map(r => r + 1)
      }
      ctx.run(q).string mustEqual "SELECT e._1 + 1 FROM (SELECT RAND() AS _1 FROM Data e) AS e WHERE e._1 > 10"
    }

    "do not double-nest" in {
      val q = quote {
        query[Data].map(e => TwoValue(e.id, infix"RAND()".as[Int])).nested.filter(r => r.value > 10).map(r => (r.id, r.value + 1))
      }
      ctx.run(q).string mustEqual "SELECT r.id, r.value + 1 FROM (SELECT e.id AS id, RAND() AS value FROM Data e) AS r WHERE r.value > 10"
    }

    "preserve nesting with single value binary op" in {
      val q = quote {
        query[Data].map(e => infix"RAND()".as[Int] + 1).filter(r => r > 10).map(r => r + 1)
      }
      ctx.run(q).string mustEqual "SELECT e._1 + 1 FROM (SELECT RAND() + 1 AS _1 FROM Data e) AS e WHERE e._1 > 10"
    }

    "preserve nesting with single value unary op" in {
      val q = quote {
        query[Data].map(e => !infix"RAND()".as[Boolean]).filter(r => r == true).map(r => !r)
      }
      ctx.run(q).string mustEqual "SELECT NOT (e._1) FROM (SELECT NOT (RAND()) AS _1 FROM Data e) AS e WHERE e._1 = true"
    }

    "preserve triple nesting with filter in between" in {
      val q = quote {
        query[Data].map(e => TwoValue(e.id, infix"RAND()".as[Int])).filter(r => r.value > 10).map(r => TwoValue(r.id, r.value + 1))
      }
      ctx.run(q).string mustEqual "SELECT e.id, e.value + 1 FROM (SELECT e.id AS id, RAND() AS value FROM Data e) AS e WHERE e.value > 10"
    }

    "preserve triple nesting with filter in between plus second filter" in {
      val q = quote {
        query[Data].map(e => TwoValue(e.id, infix"RAND()".as[Int])).filter(r => r.value > 10).map(r => TwoValue(r.id, r.value + 1)).filter(_.value > 111)
      }
      ctx.run(q).string mustEqual "SELECT e.id, e.value + 1 FROM (SELECT e.id AS id, RAND() AS value FROM Data e) AS e WHERE e.value > 10 AND (e.value + 1) > 111"
    }

    "preserve nesting of query in query" in {
      case class ThreeData(id: Int, value: Int, secondValue: Int)

      val q1 = quote {
        for {
          d <- query[Data]
        } yield TwoValue(d.id, infix"foo".as[Int])
      }

      val q2 = quote {
        for {
          r <- q1 if (r.value == 1)
        } yield ThreeData(r.id, r.value, infix"bar".as[Int])
      }

      ctx.run(q2).string mustEqual "SELECT d._1, d._2, d._3 FROM (SELECT d.id AS _1, d.value AS _2, bar AS _3 FROM (SELECT d.id AS id, foo AS value FROM Data d) AS d WHERE d.value = 1) AS d"
    }
  }
}