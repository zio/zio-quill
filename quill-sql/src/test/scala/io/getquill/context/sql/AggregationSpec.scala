package io.getquill.context.sql

import io.getquill.base.Spec
import io.getquill.norm.{DisablePhase, OptionalPhase}
import io.getquill.{Literal, MirrorSqlDialect, SqlMirrorContext, TestEntities}
import io.getquill.norm.ConfigList._

class AggregationSpec extends Spec {
  case class Person(id: Int, name: String, age: Int)
  case class PersonOpt(name: Option[String], age: Int)

  // remove the === matcher from scalatest so that we can test === in Context.extra
  override def convertToEqualizer[T](left: T): Equalizer[T] = new Equalizer(left)

  val ctx = new SqlMirrorContext(MirrorSqlDialect, Literal) with TestEntities
  import ctx._

  // an issue heavily related to the ability to use an aggregator and then a .nested after which
  // things like .filter can be called e.g:
  // .map(p => max(p.age)).nested.filter(n => ...)
  // is the fact that the max(n) will not be propagate forward out of the `nested` call.
  // That is to say, that we are guaranteed not to use normalizations that moves the max(p.age) aggregator
  // to the body of the outer query.
  // This action can be tested by a simpler operation .map(p => p.age + 1).nested.filter(p => 123)
  // If the +1 operation is propagate out, it will be moved to the outer query for example, the following
  // normalization should NOT happen:
  // SELECT p.age FROM (SELECT x.age + 1 FROM Person x) AS p WHERE p.age = 123
  //   => SELECT p.age + 1 FROM (SELECT x.age FROM Person x) AS p WHERE (p.age + 1) = 123
  // Instead it should remain as the former query
  "simple operation should not propagate from nested" in {
    ctx.run {
      query[Person].map(p => p.age + 1).nested.filter(p => p == 123)
    }.string mustEqual "SELECT p.x FROM (SELECT p.age + 1 AS x FROM Person p) AS p WHERE p.x = 123"
  }

  "aggregation functions should" - {
    "work in a map clause that is last in a query" - {
      "max" in { ctx.run(query[Person].map(p => max(p.name))).string mustEqual "SELECT MAX(p.name) FROM Person p" }
      "min" in { ctx.run(query[Person].map(p => min(p.name))).string mustEqual "SELECT MIN(p.name) FROM Person p" }
      "count" in {
        ctx.run(query[Person].map(p => count(p.name))).string mustEqual "SELECT COUNT(p.name) FROM Person p"
      }
      "avg" in { ctx.run(query[Person].map(p => avg(p.age))).string mustEqual "SELECT AVG(p.age) FROM Person p" }
      "sum" in { ctx.run(query[Person].map(p => sum(p.age))).string mustEqual "SELECT SUM(p.age) FROM Person p" }
    }

    "work correctly with a filter cause that is BEFORE the aggregation" in {
      val q = quote {
        query[Person].filter(p => p.name == "Joe").map(p => (p.id, max(p.name)))
      }
      ctx.run(q).string mustEqual "SELECT p.id AS _1, MAX(p.name) AS _2 FROM Person p WHERE p.name = 'Joe'"
    }

    "force nesting in a map clause that NOT last in a query" in {
      val q = quote {
        query[Person].map(p => (p.id, max(p.name))).filter(p => p._1 == 123)
      }
      ctx.run(q).string mustEqual "SELECT p._1, p._2 FROM (SELECT p.id AS _1, MAX(p.name) AS _2 FROM Person p) AS p WHERE p._1 = 123"
    }

    "work with optional mapping" in {
      val q = quote {
        query[PersonOpt].map(p => p.name.map(n => max(n)))
      }
      ctx.run(q).string mustEqual "SELECT MAX(p.name) FROM PersonOpt p"
    }
    "work with optional mapping - external" in {
      val q = quote {
        query[PersonOpt].map(p => max(p.name))
      }
      ctx.run(q).string mustEqual "SELECT MAX(p.name) FROM PersonOpt p"
    }
    "work with optional mapping - external (avg)" in {
      val q = quote {
        query[PersonOpt].map(p => avg(p.age))
      }
      ctx.run(q).string mustEqual "SELECT AVG(p.age) FROM PersonOpt p"
    }
    "work with optional mapping + nested + filter" in {
      val q = quote {
        query[PersonOpt].map(p => p.name.map(n => max(n))).nested.filter(p => p == Option("Joe"))
      }
      ctx.run(q).string mustEqual "SELECT p.x FROM (SELECT MAX(p.name) AS x FROM PersonOpt p) AS p WHERE p.x = 'Joe'"
    }

    "work externally with optional mapping" - {
      "max" in {
        ctx.run(query[PersonOpt].map(p => max(p.name))).string mustEqual "SELECT MAX(p.name) FROM PersonOpt p"
      }
      "min" in {
        ctx.run(query[PersonOpt].map(p => min(p.name))).string mustEqual "SELECT MIN(p.name) FROM PersonOpt p"
      }
      "count" in {
        ctx.run(query[PersonOpt].map(p => count(p.name))).string mustEqual "SELECT COUNT(p.name) FROM PersonOpt p"
      }
      "avg" in {
        ctx.run(query[PersonOpt].map(p => avg(p.age))).string mustEqual "SELECT AVG(p.age) FROM PersonOpt p"
      }
      "sum" in {
        ctx.run(query[PersonOpt].map(p => sum(p.age))).string mustEqual "SELECT SUM(p.age) FROM PersonOpt p"
      }
    }
  }

  "groupByMap should" - {
    "work in the simple form" - {
      "max" in {
        ctx.run {
          query[Person].groupByMap(p => p.id)(p => (p.name, max(p.name)))
        }.string mustEqual "SELECT p.name AS _1, MAX(p.name) AS _2 FROM Person p GROUP BY p.id"
      }
      "min" in {
        ctx.run {
          query[Person].groupByMap(p => p.id)(p => (p.name, min(p.name)))
        }.string mustEqual "SELECT p.name AS _1, MIN(p.name) AS _2 FROM Person p GROUP BY p.id"
      }
      "count" in {
        ctx.run {
          query[Person].groupByMap(p => p.id)(p => (p.name, count(p.name)))
        }.string mustEqual "SELECT p.name AS _1, COUNT(p.name) AS _2 FROM Person p GROUP BY p.id"
      }
      "avg" in {
        ctx.run {
          query[Person].groupByMap(p => p.id)(p => (p.name, avg(p.age)))
        }.string mustEqual "SELECT p.name AS _1, AVG(p.age) AS _2 FROM Person p GROUP BY p.id"
      }
      "sum" in {
        ctx.run {
          query[Person].groupByMap(p => p.id)(p => (p.name, sum(p.age)))
        }.string mustEqual "SELECT p.name AS _1, SUM(p.age) AS _2 FROM Person p GROUP BY p.id"
      }
    }

    "work with multiple aggregators" in {
      ctx.run(query[Person].groupByMap(p => p.id)(p => (p.name, max(p.name), avg(p.age)))).string mustEqual
        "SELECT p.name AS _1, MAX(p.name) AS _2, AVG(p.age) AS _3 FROM Person p GROUP BY p.id"
    }

    "work with a filter clause after" in {
      ctx.run {
        query[Person].groupByMap(p => p.id)(p => (p.name, max(p.name))).filter(p => p._2 == "Joe")
      }.string mustEqual
        "SELECT p._1, p._2 FROM (SELECT p.name AS _1, MAX(p.name) AS _2 FROM Person p GROUP BY p.id) AS p WHERE p._2 = 'Joe'"
    }

    "work with a filter clause before" in {
      ctx.run {
        query[Person].filter(p => p.name == "Joe").groupByMap(p => p.id)(p => (p.name, max(p.name)))
      }.string mustEqual
        "SELECT p.name AS _1, MAX(p.name) AS _2 FROM Person p WHERE p.name = 'Joe' GROUP BY p.id"
    }

    "work with a groupByMap(to-leaf).filter" in {
      ctx.run(query[Person].groupByMap(p => p.age)(p => max(p.age)).filter(a => a > 1000)).string mustEqual
        "SELECT p.x FROM (SELECT MAX(p.age) AS x FROM Person p GROUP BY p.age) AS p WHERE p.x > 1000"
    }

    "work with a map(to-leaf).groupByMap.filter" in {
      ctx.run(query[Person].map(p => p.age).groupByMap(p => p)(p => max(p)).filter(a => a > 1000)).string mustEqual
        "SELECT p.x FROM (SELECT MAX(p.age) AS x FROM Person p GROUP BY p.age) AS p WHERE p.x > 1000"
    }

    // Disable thte apply-map phase to make sure these work in cases where this reduction is not possible (e.g. where they use infix etc...).
    // Infix has a special case already so want to not use that specifically.
    "work with a map(to-leaf).groupByMap.filter - no ApplyMap" in {
      implicit val d = new DisablePhase { override type Phase = OptionalPhase.ApplyMap :: HNil }
      ctx.run(query[Person].map(p => p.age).groupByMap(p => p)(p => max(p)).filter(a => a > 1000)).string mustEqual
        "SELECT p.x FROM (SELECT MAX(p.age) AS x FROM (SELECT p.age FROM Person p) AS p GROUP BY p.age) AS p WHERE p.x > 1000"
    }

    case class NameAge(name: String, age: Int)

    "work with map(product).filter.groupByMap.filter" in {
      ctx.run {
        query[Person]
          .map(p => NameAge(p.name, p.age))
          .filter(t => t.age == 123)
          .groupByMap(t => t.name)(t => (t.name, max(t.age)))
      }.string mustEqual
        "SELECT p.name AS _1, MAX(p.age) AS _2 FROM Person p WHERE p.age = 123 GROUP BY p.name"
    }

    "work with map(product).filter.groupByMap.filter - no ApplyMap" in {
      implicit val d = new DisablePhase { override type Phase = OptionalPhase.ApplyMap :: HNil }
      ctx.run {
        query[Person]
          .map(p => NameAge(p.name, p.age))
          .filter(t => t.age == 123)
          .groupByMap(t => t.name)(t => (t.name, max(t.age)))
      }.string mustEqual
        "SELECT p.name AS _1, MAX(p.age) AS _2 FROM (SELECT p.name, p.age FROM Person p) AS p WHERE p.age = 123 GROUP BY p.name"
    }

    "work in a for-comprehension" in { // //
      case class Address(id: Int, owner: Int, street: String)
      case class Furniture(owner: Int, location: Int)
      case class PersonInfo(id: Int, maxAge: Int)
      import extras._

      ctx.run {
        for {
          a <- query[Address]
          p <- query[Person]
                 .groupByMap(p => p.name == "Joe")(p => PersonInfo(p.id, max(p.age)))
                 .leftJoin(p => p.id == a.owner)
          f <- query[Furniture].leftJoin(f => f.owner === p.map(_.id) && f.location == a.id)
        } yield (a, p, f)
      }.string mustEqual
        "SELECT a.id, a.owner, a.street, p.id, p.maxAge, f.owner, f.location FROM Address a LEFT JOIN (SELECT p.id, MAX(p.age) AS maxAge FROM Person p GROUP BY p.name = 'Joe') AS p ON p.id = a.owner LEFT JOIN Furniture f ON f.owner = p.id AND f.location = a.id"
    }

  }
}
