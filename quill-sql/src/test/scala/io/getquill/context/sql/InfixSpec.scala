package io.getquill.context

import io.getquill.SqlMirrorContext
import io.getquill.MirrorSqlDialect
import io.getquill.TestEntities
import io.getquill.Literal
import io.getquill.base.Spec
import io.getquill.norm.EnableTrace
import io.getquill.util.Messages.TraceType

class InfixSpec extends Spec { // //

  "queries with infix should" - {

    val ctx = new SqlMirrorContext(MirrorSqlDialect, Literal) with TestEntities
    import ctx._

    case class Data(id: Int)
    case class TwoValue(id: Int, value: Int)

    "preserve nesting where needed" in {
      val q = quote {
        query[Data].map(e => TwoValue(e.id, sql"RAND()".as[Int])).filter(r => r.value > 10)
      }
      ctx
        .run(q)
        .string mustEqual "SELECT e.id, e.value FROM (SELECT e.id, RAND() AS value FROM Data e) AS e WHERE e.value > 10"
    }

    "collapse nesting where not needed" in {
      val q = quote {
        query[Data].map(e => TwoValue(e.id, sql"SOMETHINGPURE()".pure.as[Int])).filter(r => r.value > 10)
      }
      ctx.run(q).string mustEqual "SELECT e.id, SOMETHINGPURE() AS value FROM Data e WHERE SOMETHINGPURE() > 10"
    }

    "preserve nesting with single value" in {
      val q = quote {
        query[Data].map(e => sql"RAND()".as[Int]).filter(r => r > 10).map(r => r + 1)
      }
      ctx.run(q).string mustEqual "SELECT e.x + 1 FROM (SELECT RAND() AS x FROM Data e) AS e WHERE e.x > 10"
    }

    "do not double-nest" in {
      val q = quote {
        query[Data]
          .map(e => TwoValue(e.id, sql"RAND()".as[Int]))
          .nested
          .filter(r => r.value > 10)
          .map(r => (r.id, r.value + 1))
      }
      ctx.run(q).string mustEqual "SELECT r.id AS _1, r.value + 1 AS _2 FROM (SELECT e.id, RAND() AS value FROM Data e) AS r WHERE r.value > 10"
    }

    "preserve nesting with single value binary op" in {
      val q = quote {
        query[Data].map(e => sql"RAND()".as[Int] + 1).filter(r => r > 10).map(r => r + 1)
      }
      ctx.run(q).string mustEqual "SELECT e.x + 1 FROM (SELECT RAND() + 1 AS x FROM Data e) AS e WHERE e.x > 10"
    }

    "preserve nesting with single value unary op" in {
      val q = quote {
        query[Data].map(e => !sql"RAND()".as[Boolean]).filter(r => r == true).map(r => !r)
      }
      ctx.run(q).string mustEqual "SELECT NOT (e.x) FROM (SELECT NOT (RAND()) AS x FROM Data e) AS e WHERE e.x = true"
    }

    "preserve triple nesting with filter in between" in {
      val q = quote {
        query[Data]
          .map(e => TwoValue(e.id, sql"RAND()".as[Int]))
          .filter(r => r.value > 10)
          .map(r => TwoValue(r.id, r.value + 1))
      }
      ctx.run(q).string mustEqual "SELECT e.id, e.value + 1 AS value FROM (SELECT e.id, RAND() AS value FROM Data e) AS e WHERE e.value > 10"
    }

    "preserve triple nesting with filter in between plus second filter" in {
      val q = quote {
        query[Data]
          .map(e => TwoValue(e.id, sql"RAND()".as[Int]))
          .filter(r => r.value > 10)
          .map(r => TwoValue(r.id, r.value + 1))
          .filter(_.value > 111)
      }
      ctx.run(q).string mustEqual "SELECT e.id, e.value + 1 AS value FROM (SELECT e.id, RAND() AS value FROM Data e) AS e WHERE e.value > 10 AND (e.value + 1) > 111"
    }

    "preserve nesting of query in query" in {
      case class ThreeData(id: Int, value: Int, secondValue: Int)

      val q1 = quote {
        for {
          d <- query[Data]
        } yield TwoValue(d.id, sql"foo".as[Int])
      }

      val q2 = quote {
        for {
          r <- q1 if (r.value == 1)
        } yield ThreeData(r.id, r.value, sql"bar".as[Int])
      }
      ctx.run(q2).string mustEqual "SELECT d.id, d.value, bar AS secondValue FROM (SELECT d.id, foo AS value FROM Data d) AS d WHERE d.value = 1"
    }

    "excluded infix values" - {
      case class Person(id: Int, name: String, other: String, other2: String)

      "should not be dropped" in {
        val q = quote {
          query[Person].map(p => (p.name, p.id, sql"foo(${p.other})".as[Int])).map(p => (p._1, p._2))
        }

        ctx.run(q).string mustEqual "SELECT p._1, p._2 FROM (SELECT p.name AS _1, p.id AS _2, foo(p.other) AS _3 FROM Person p) AS p"
      }

      "should not be dropped if pure" in {
        val q = quote {
          query[Person].map(p => (p.name, p.id, sql"foo(${p.other})".pure.as[Int])).map(p => (p._1, p._2))
        }

        ctx.run(q).string mustEqual "SELECT p.name AS _1, p.id AS _2 FROM Person p"
      }

      "should not be dropped in nested tuples" in {
        val q = quote {
          query[Person].map(p => (p.name, (p.id, sql"foo(${p.other})".as[Int]))).map(p => (p._1, p._2._1))
        }

        ctx.run(q).string mustEqual "SELECT p._1, p._2_1 AS _2 FROM (SELECT p.name AS _1, p.id AS _2_1, foo(p.other) AS _2_2 FROM Person p) AS p"
      }

      "should not be selected twice if in sub-sub tuple" in {
        val q = quote {
          query[Person].map(p => (p.name, (p.id, sql"foo(${p.other})".as[Int]))).map(p => (p._1, p._2))
        }

        ctx.run(q).string mustEqual "SELECT p._1, p._2_1 AS _1, p._2_2 AS _2 FROM (SELECT p.name AS _1, p.id AS _2_1, foo(p.other) AS _2_2 FROM Person p) AS p"
      }

      "should not be selected in sub-sub tuple if pure" in {
        val q = quote {
          query[Person].map(p => (p.name, (p.id, sql"foo(${p.other})".pure.as[Int]))).map(p => (p._1, p._2))
        }

        ctx.run(q).string mustEqual "SELECT p.name AS _1, p.id AS _1, foo(p.other) AS _2 FROM Person p"
      }

      "should not be selected twice in one field matched, one missing" in {
        val q = quote {
          query[Person]
            .map(p => (p.name, (p.id, sql"foo(${p.other}, ${p.other2})".as[Int], p.other)))
            .map(p => (p._1, p._2._1, p._2._3))
        }

        ctx.run(q).string mustEqual "SELECT p._1, p._2_1 AS _2, p._2_3 AS _3 FROM (SELECT p.name AS _1, p.id AS _2_1, foo(p.other, p.other2) AS _2_2, p.other AS _2_3 FROM Person p) AS p"
      }

      "distinct-on infix example" in {
        val q = quote {
          query[Person].map(p => (sql"DISTINCT ON (${p.other})".as[Int], p.name, p.id)).map(t => (t._2, t._3))
        }

        ctx.run(q).string mustEqual "SELECT p._2 AS _1, p._3 AS _2 FROM (SELECT DISTINCT ON (p.other) AS _1, p.name AS _2, p.id AS _3 FROM Person p) AS p"
      }
    }
  }
}
