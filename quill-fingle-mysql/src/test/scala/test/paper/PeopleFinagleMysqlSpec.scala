package test.paper

import io.getquill.impl.Source
import test.Spec
import io.getquill._
import io.getquill.finagle.mysql.FinagleMysqlSource
import com.twitter.util.Await
import com.twitter.util.Future

class PeopleFinagleMysqlSpec extends Spec {

  object peopleDB extends FinagleMysqlSource

  case class Person(name: String, age: Int)
  case class Couple(her: String, him: String)

  def await[T](future: Future[T]) = Await.result(future)

  "Example 1 - diferences" in {

    val differences =
      quote {
        for {
          c <- from[Couple]
          w <- from[Person]
          m <- from[Person] if (c.her == w.name && c.him == m.name && w.age > m.age)
        } yield {
          (w.name, w.age - m.age)
        }
      }

    await(peopleDB.run(differences)) mustEqual List(("Alex", 5), ("Cora", 2))
  }

  "Example 2 - range simple" in {

    val rangeSimple = quote {
      (a: Int, b: Int) =>
        for {
          u <- from[Person] if (a <= u.age && u.age < b)
        } yield {
          u
        }
    }
    val a = 30
    val b = 40
    await(peopleDB.run(rangeSimple)(a, b)) mustEqual List(Person("Cora", 33), Person("Drew", 31))
  }

  "Examples 3, 4 - satisfies" in {
    val satisfies =
      quote {
        (p: Int => Boolean) =>
          for {
            u <- from[Person] if (p(u.age))
          } yield {
            u
          }
      }

    await(peopleDB.run(satisfies((x: Int) => 20 <= x && x < 30))) mustEqual List(Person("Edna", 21))
    await(peopleDB.run(satisfies((x: Int) => x % 2 == 0))) mustEqual List(Person("Alex", 60), Person("Fred", 60))
  }

  "Example 5 - composee" in {
    val range = quote {
      (a: Int, b: Int) =>
        for {
          u <- from[Person] if (a <= u.age && u.age < b)
        } yield {
          u
        }
    }

    val ageFromName = quote {
      (s: String) =>
        for {
          u <- from[Person] if (s == u.name)
        } yield {
          u.age
        }
    }

    val compose = quote {
      (s: String, t: String) =>
        for {
          a <- ageFromName(s)
          b <- ageFromName(t)
          r <- range(a, b)
        } yield {
          r
        }
    }

    await(peopleDB.run(compose("Drew", "Bert"))) mustEqual List(Person("Cora", 33), Person("Drew", 31))
  }

}
