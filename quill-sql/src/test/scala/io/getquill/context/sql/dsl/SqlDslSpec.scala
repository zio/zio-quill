package io.getquill.context.sql.dsl

import io.getquill.{ Insert, Query, Quoted, Spec }
import io.getquill.context.sql.testContext
import io.getquill.context.sql.testContext._

class SqlDslSpec extends Spec {

  "like" - {
    "constant" in {
      val q = quote {
        query[TestEntity].filter(t => Like(t.s) like "a")
      }
      testContext.run(q).string mustEqual "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t WHERE t.s like 'a'"
    }
    "string interpolation" in {
      val q = quote {
        query[TestEntity].filter(t => t.s like s"%${lift("a")}%")
      }
      testContext.run(q).string mustEqual "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t WHERE t.s like ('%' || ?) || '%'"
    }
  }

  "forUpdate" in {
    val q: Quoted[Query[TestEntity]] = quote {
      query[TestEntity].filter(t => t.s == "a").forUpdate
    }
    testContext.run(q).string mustEqual "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t WHERE t.s = 'a' FOR UPDATE"
  }

  case class Person(name: String, age: Int)

  "insert with subselects" - {
    // In this case we are not renaming the ages into the table, just the names so technically
    // we can exclude the age columns. I am not sure if that has some other potential issues
    // for inserts so for now I am just doing ExpandNestedSelects and RemoveUnusedAliases.
    // have a look at `astTokenizer` in `SqlIdiom` for details.
    val q1 = quote {
      (query[Person].map(p => Person("x", p.age)) union query[Person]).map(_.name)
    }

    val q2 = quote {
      infix"INSERT into names $q1".as[Insert[Person]]
    }

    "should show all field aliases" in {
      testContext.run(q2).string mustEqual "INSERT into names SELECT x1.name FROM ((SELECT 'x' AS name, p.age FROM Person p) UNION (SELECT x.name, x.age FROM Person x)) AS x1"
    }
  }
}
