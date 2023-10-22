package io.getquill.context.sql.norm

import io.getquill.base.Spec
import io.getquill.norm.SheathLeafClausesApply
import io.getquill.util.TraceConfig
import io.getquill.{MirrorSqlDialect, Query, Quoted, SnakeCase, SqlMirrorContext}

class SheathLeafClausesSpec extends Spec {

  val ctx = new SqlMirrorContext(MirrorSqlDialect, SnakeCase)
  import ctx._
  case class Person(firstName: String, lastName: String, age: Int)
  val SheathLeafClauses = new SheathLeafClausesApply(TraceConfig.Empty)

  object Wrap {
    case class Age(age: Int)
    object Arr {
      case class X(x: Array[String])
    }
    object Int {
      case class X(x: Int)
      case class I(i: Int)
      case class U(u: Int)
    }
    case class Data(id: Int)
  }

  "sheath leaf clause in" - {
    "map(=>leaf).groupBy.map" in {
      // this works even without SheathLeafClauses but is good test of the SheathLeafClauses
      val q = quote {
        query[Person].map(p => p.age).groupBy(p => p).map(p => p._2.max)
      }
      val c = quote {
        // Note that the intermediate wrap.age is not needed but added during a Wrap-Sheaths phase. Should possibly remove it as an optimization.
        query[Person].map(p => Wrap.Age(p.age)).groupBy(p => p.age).map(p => p._2.map(p => p.age).max)
      }
      SheathLeafClauses.apply(q.ast) mustEqual c.ast
      ctx.run(q).string mustEqual ctx.run(c).string
    }

    "map(infix.as[Int]).groupBy.map" in {
      // this works even without SheathLeafClauses but is good test of the SheathLeafClauses
      val q = quote {
        // also this works even without SheathLeafClauses but is good test of the SheathLeafClauses transformation
        query[Person].map(p => p.age + sql"foo".as[Int]).groupBy(p => p).map(p => p._2.max)
      }
      val c = quote {
        query[Person].map(p => Wrap.Int.X(p.age + sql"foo".as[Int])).groupBy(p => p.x).map(p => p._2.map(p => p.x).max)
      }
      SheathLeafClauses.apply(q.ast) mustEqual c.ast
      // TODO Replace all p.x with p._1 in c then can use this
      // printIfNotSameString(run(q).string, run(c).string)
    }

    "infix.as[Query[leaf]].groupBy.map" in {
      val q = quote {
        sql"leaf".as[Query[Int]].groupBy(p => p).map(p => p._2.max)
      }
      val c = quote {
        sql"leaf".as[Query[Int]].map(i => Wrap.Int.I(i)).groupBy(p => p.i).map(p => p._2.map(p => p.i).max)
      }
      SheathLeafClauses.apply(q.ast) mustEqual c.ast
      ctx.run(q).string mustEqual ctx.run(c).string
    }

    // TODO This should actually live the quill-jdbc module and be executed since it should be a working example
    "(example) infix.as[Query[leaf]].groupBy.map" in {
      val q = quote {
        sql"unnest(array['foo','bar'])".as[Query[Int]].groupBy(p => p).map(p => p._2.max)
      }
      val c = quote {
        sql"unnest(array['foo','bar'])"
          .as[Query[Int]]
          .map(i => Wrap.Int.I(i))
          .groupBy(p => p.i)
          .map(p => p._2.map(p => p.i).max)
      }
      SheathLeafClauses.apply(q.ast) mustEqual c.ast
      ctx.run(q).string mustEqual ctx.run(c).string
    }

    "unions" - {
      "(map(leaf) unionAll map(leaf)).agg" in {
        val q = quote {
          (query[Person].map(p => p.age) ++ query[Person].map(p => p.age)).max
        }

        val c = quote {
          (query[Person].map(p => Wrap.Age(p.age)) ++ query[Person].map(p => Wrap.Age(p.age))).map(e => e.age).max
        }
        ctx.run(q).string mustEqual ctx.run(c).string
        SheathLeafClauses.apply(q.ast) mustEqual c.ast
      }

      // Same as before but union
      "(map(leaf) union map(leaf)).agg" in {
        val q = quote {
          (query[Person].map(p => p.age) union query[Person].map(p => p.age)).max
        }

        val c = quote {
          (query[Person].map(p => Wrap.Age(p.age)) union query[Person].map(p => Wrap.Age(p.age))).map(e => e.age).max
        }
        ctx.run(q).string mustEqual ctx.run(c).string
        SheathLeafClauses.apply(q.ast) mustEqual c.ast
      }

      // Technically this worked before SheathLeafClauses was introduced but this kind of query is impacted so test it here
      "concatMap(leaf).join(concatMap(leaf))" in {
        val q: Quoted[Query[(String, String)]] = quote(
          query[Person]
            .concatMap(p => p.firstName.split(" "))
            .join(query[Person].concatMap(t => t.firstName.split(" ")))
            .on { case (a, b) => a == b }
        )
        // TODO star identifiers should not have aliases
        ctx.run(q).string mustEqual "SELECT x01.*, x11.* FROM (SELECT UNNEST(SPLIT(p.first_name, ' ')) AS x FROM person p) AS x01 INNER JOIN (SELECT UNNEST(SPLIT(t.first_name, ' ')) AS x FROM person t) AS x11 ON x01.x = x11.x"
      }

      "(map(leaf) union map(computed-leaf)).agg" in {
        val q = quote {
          (query[Person].map(p => p.age) union query[Person].map(p => p.age + 123)).max
        }

        val c = quote {
          (query[Person].map(p => Wrap.Age(p.age)).map(e => Wrap.Int.U(e.age)) union query[Person]
            .map(p => Wrap.Int.X(p.age + 123))
            .map(e => Wrap.Int.U(e.x))).map(e => e.u).max
        }
        ctx.run(q).string mustEqual ctx.run(c).string
        SheathLeafClauses.apply(q.ast) mustEqual c.ast
      }

      "(map(computed-leaf) unionAll map(computed-leaf)).agg" in {
        val q = quote {
          (query[Person].map(p => p.age + 123) ++ query[Person].map(p => p.age + 456)).max
        }

        val c = quote {
          (query[Person].map(p => Wrap.Int.X(p.age + 123)) ++ query[Person].map(p => Wrap.Int.X(p.age + 456)))
            .map(e => e.x)
            .max
        }
        ctx.run(q).string mustEqual ctx.run(c).string
        SheathLeafClauses.apply(q.ast) mustEqual c.ast
      }

      // new test
      "(map(=>node).groupBy.map(_1)) unionAll (map(=>node).groupBy.map(_1))" in {
        val q = quote {
          query[Person].groupBy(p => p.age).map(ap => ap._1) ++ query[Person].groupBy(p => p.age).map(ap => ap._1)
        }
        ctx.run(q).string mustEqual "(SELECT p.age FROM person p GROUP BY p.age) UNION ALL (SELECT p1.age FROM person p1 GROUP BY p1.age)"
      }
      "(map(=>node).groupBy.map) unionAll (map(=>node).groupBy.map)" in {
        val q = quote {
          query[Person].groupBy(p => p.age).map(ap => ap._2.max) ++ query[Person]
            .groupBy(p => p.age)
            .map(ap => ap._2.max)
        }
        ctx.run(q).string mustEqual "(SELECT MAX(p.*) FROM person p GROUP BY p.age) UNION ALL (SELECT MAX(p1.*) FROM person p1 GROUP BY p1.age)"
      }

      "(map(=>node).groupBy.map(Wrap)) unionAll (map(=>node).groupBy.map(Wrap))" in {
        val q = quote {
          query[Person]
            .groupBy(p => Wrap.Age(p.age))
            .map(ap => ap._2.map(p => Wrap.Age(p.age)).map(p => p.age).max) ++ query[Person]
            .groupBy(p => p.age)
            .map(ap => ap._2.map(p => Wrap.Age(p.age)).map(p => p.age).max)
        }
        ctx.run(q).string mustEqual "(SELECT MAX(p.age) FROM person p GROUP BY p.age) UNION ALL (SELECT MAX(p2.age) FROM person p2 GROUP BY p2.age)"
      }

      "(map(leaf).concatMap unionAll map(leaf).concatMap).agg" in {
        val q = quote {
          (query[Person].concatMap(p => p.firstName.split("a")) ++ query[Person].concatMap(p =>
            p.firstName.split("b")
          )).max
        }
        ctx.run(q).string mustEqual "SELECT MAX(e.x) FROM ((SELECT UNNEST(SPLIT(p.first_name, 'a')) AS x FROM person p) UNION ALL (SELECT UNNEST(SPLIT(p1.first_name, 'b')) AS x FROM person p1)) AS e"

        // Can't evaluate this query due to typing issues but still is useful to AST comparison
        implicit def iterableContainer: Wrap.Arr.X => Iterable[Wrap.Arr.X] = ???

        val c = quote {
          (query[Person].concatMap(p => Wrap.Arr.X(p.firstName.split("a"))) ++ query[Person].concatMap(p =>
            Wrap.Arr.X(p.firstName.split("b"))
          )).map(e => e.x).max
        }
        SheathLeafClauses.apply(q.ast) mustEqual c.ast
      }
    }

    "map(computed-leaf).filter" in {
      val q = quote {
        query[Person].map(p => p.age + 123).filter(a => a > 123)
      }
      val c = quote {
        query[Person].map(p => Wrap.Int.X(p.age + 123)).filter(a => a.x > 123)
      }
      SheathLeafClauses.apply(q.ast) mustEqual c.ast
    }

    "map(leaf).filter" in {
      val q = quote {
        query[Person].map(p => p.age).filter(a => a > 123)
      }
      val c = quote {
        query[Person].map(p => Wrap.Age(p.age)).filter(a => a.age > 123)
      }
      SheathLeafClauses.apply(q.ast) mustEqual c.ast
    }
  }
}
