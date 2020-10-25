package io.getquill.context.spark

import io.getquill.Spec

case class Person(name: String, age: Int)
case class Couple(her: String, him: String)

class PeopleJdbcSpec extends Spec {

  val context = io.getquill.context.sql.testContext

  import testContext._
  import sqlContext.implicits._

  val couples = liftQuery {
    Seq(
      Couple("Alex", "Bert"),
      Couple("Cora", "Drew"),
      Couple("Edna", "Fred")
    ).toDS
  }

  val people = liftQuery {
    Seq(
      Person("Alex", 60),
      Person("Bert", 55),
      Person("Cora", 33),
      Person("Drew", 31),
      Person("Edna", 21),
      Person("Fred", 60)
    ).toDS
  }

  "Example 1 - differences" in {
    val q =
      quote {
        for {
          c <- couples.distinct
          w <- people.distinct
          m <- people.distinct if (c.her == w.name && c.him == m.name && w.age > m.age)
        } yield {
          (w.name, w.age - m.age)
        }
      }
    testContext.run(q).collect.toList.sorted mustEqual
      List(("Alex", 5), ("Cora", 2))
  }

  "Example 1 - differences with explicit join" in {
    val q =
      quote {
        for {
          c <- couples
          w <- people.join(w => c.her == w.name)
          m <- people.join(m => c.him == m.name) if (w.age > m.age)
        } yield {
          (w.name, w.age - m.age)
        }
      }
    testContext.run(q).collect.toList.sorted mustEqual
      List(("Alex", 5), ("Cora", 2))
  }

  "Example 2 - range simple" in {
    val rangeSimple = quote {
      (a: Int, b: Int) =>
        for {
          u <- people if (a <= u.age && u.age < b)
        } yield {
          u
        }
    }

    testContext.run(rangeSimple(30, 40)).collect.toList mustEqual
      List(Person("Cora", 33), Person("Drew", 31))
  }

  val satisfies =
    quote {
      (p: Int => Boolean) =>
        for {
          u <- people if (p(u.age))
        } yield {
          u
        }
    }

  "Example 3 - satisfies" in {
    testContext.run(satisfies((x: Int) => 20 <= x && x < 30)).collect.toList mustEqual
      List(Person("Edna", 21))
  }

  "Example 4 - satisfies" in {
    testContext.run(satisfies((x: Int) => x % 2 == 0)).collect.toList mustEqual
      List(Person("Alex", 60), Person("Fred", 60))
  }

  "Example 5 - compose" in {
    val q = {
      val range = quote {
        (a: Int, b: Int) =>
          for {
            u <- people if (a <= u.age && u.age < b)
          } yield {
            u
          }
      }
      val ageFromName = quote {
        (s: String) =>
          for {
            u <- people if (s == u.name)
          } yield {
            u.age
          }
      }
      quote {
        (s: String, t: String) =>
          for {
            a <- ageFromName(s)
            b <- ageFromName(t)
            r <- range(a, b)
          } yield {
            r
          }
      }
    }
    testContext.run(q("Drew", "Bert")).collect.toList mustEqual
      List(Person("Cora", 33), Person("Drew", 31))
  }

  "Distinct" - {
    "simple distinct" in {
      val q =
        quote {
          couples.distinct
        }
      testContext.run(q).collect.toList mustEqual
        List(Couple("Alex", "Bert"), Couple("Cora", "Drew"), Couple("Edna", "Fred"))
    }
    "complex distinct" in {
      val q =
        quote {
          for {
            c <- couples.distinct
            w <- people.distinct.join(w => c.her == w.name)
            m <- people.distinct.join(m => c.him == m.name) if (w.age > m.age)
          } yield {
            (w.name, w.age - m.age)
          }
        }
      testContext.run(q).collect.toList.sorted mustEqual
        List(("Alex", 5), ("Cora", 2))
    }
    "should throw Exception" in {
      val q =
        """ quote {
          for {
            c <- couples
            w <- people.join(w => c.her == w.name).distinct
            m <- people.join(m => c.him == m.name) if (w.age > m.age)
          } yield {
            (w.name, w.age - m.age)
          }
        }""" mustNot compile
    }
  }

  "Nested" - {
    "simple nested" in {
      val q =
        quote {
          couples.nested
        }
      testContext.run(q.dynamic).collect.toList mustEqual
        List(Couple("Alex", "Bert"), Couple("Cora", "Drew"), Couple("Edna", "Fred"))
    }
    "complex distinct" in {
      val q =
        quote {
          for {
            c <- couples.nested
            w <- people.nested.join(w => c.her == w.name)
            m <- people.nested.join(m => c.him == m.name) if (w.age > m.age)
          } yield {
            (w.name, w.age - m.age)
          }
        }
      testContext.run(q).collect.toList.sorted mustEqual
        List(("Alex", 5), ("Cora", 2))
    }
    "should throw Exception" in {
      val q =
        """ quote {
          for {
            c <- couples
            w <- people.join(w => c.her == w.name).nested
            m <- people.join(m => c.him == m.name) if (w.age > m.age)
          } yield {
            (w.name, w.age - m.age)
          }
        }""" mustNot compile
    }
  }
}
