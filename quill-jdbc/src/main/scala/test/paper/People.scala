package test.paper

import io.getquill.Source
import io.getquill.jdbc.JdbcSource

object People extends App {

  object peopleDB extends JdbcSource {

    val people = entity[Person]
    val couples = entity[Couple]
  }

  case class Person(name: String, age: Int)
  case class Couple(her: String, him: String)

  // Example 1

  val differences =
    for {
      c <- peopleDB.couples
      w <- peopleDB.people
      m <- peopleDB.people if (c.her == w.name && c.him == m.name && w.age > m.age)
    } yield {
      (w.name, w.age - m.age)
    }

  println(differences.run)

}