package test.paper

import io.getquill.Source
import io.getquill.Partial
import io.getquill.jdbc.JdbcSource
import test.Spec
import io.getquill.Queryable
import io.getquill.Queryable

class PeopleSpec extends Spec {

  object peopleDB extends JdbcSource

  case class Person(name: String, age: Int)
  case class Couple(her: String, him: String)

  "Example 1 - diferences" in {

    val differences =
      for {
        c <- Queryable[Couple]
        w <- Queryable[Person]
        m <- Queryable[Person] if (c.her == w.name && c.him == m.name && w.age > m.age)
      } yield {
        (w.name, w.age - m.age)
      }

    peopleDB.run(differences) mustEqual List(("Alex", 5), ("Cora", 2))
  }

  "Example 2 - range simple" in {

    val rangeSimple = Partial {
      (a: Int, b: Int) =>
        for {
          u <- Queryable[Person] if (a <= u.age && u.age < b)
        } yield {
          u
        }
    }

    val r = rangeSimple(30, 40)

    peopleDB.run(r) mustEqual List(Person("Cora", 33), Person("Drew", 31))
  }

  //  "Example 3 - satisfies" in {
  //     for u in db.People do
  //                        if p u.Age then
  //                            yield u
  //     
  //    val satisfies =
  //      Partial {
  //        (p: Int => Boolean) =>
  //          for {
  //            u <- peopleDB.people if (p(u.age))
  //          } yield {
  //            u
  //          }
  //      }
  //  }
}
