package io.getquill

import language.experimental.macros
import io.getquill.jdbc.JdbcSource
import java.sql.ResultSet

case class Person(id: Long, name: String, surname: String, age: Int)
case class Address(id: Long, personId: Long, streetId: Long, number: Int)
case class Street(id: Long, name: String, city: String)

object Test extends App {

  object db extends JdbcSource

  def q1 =
    Queryable[Person].filter(p => p.name == p.surname).map(p => (p.name, p.age))
  db.run(q1)

  def q2 =
    for {
      p <- Queryable[Person] if (p.name == null)
      a <- Queryable[Address] if (a.personId == p.id)
    } yield {
      a
    }

  db.run(q2)

  def q3 =
    for {
      a <- q2
      s <- Queryable[Street] if (a.streetId == s.id)
    } yield {
      a
    }
  db.run(q3)

  val byName = Partial {
    (name: String) => Queryable[Person].filter(_.name == name)
  }

  val q4 =
    byName("jesus")
  db.run(q4)

  val byFullName = Partial {
    (name: String, surname: String) => byName(name).filter(_.surname == surname)
  }

  val q5 =
    byFullName("flavio", "brasil")
  db.run(q5)

  val nameEqualsSurname = Partial {
    (p: Person) => p.name == p.surname
  }

  val q6 =
    Queryable[Person].filter(nameEqualsSurname(_))
  db.run(q6)

  val nameIs = Partial {
    (p: Person, name: String) => p.name == name
  }

  val q7 =
    Queryable[Person].filter(nameIs(_, "flavio"))
  db.run(q7)

  val names =
    Queryable[Person].map(_.name)

  val q8 =
    for {
      name <- names
      p <- Queryable[Person] if (p.name == name)
    } yield {
      (p.name, p.age)
    }
  db.run(q8)

  val q9 =
    Queryable[Address].map(_.personId)
  db.run(q9)

  val q10 =
    for {
      p1 <- Queryable[Person]
      p2 <- Queryable[Person] if (p1.name == p2.name)
    } yield {
      p2
    }
  db.run(q10)

  val personAndAddress =
    for {
      p <- Queryable[Person]
      a <- Queryable[Address] if (a.personId == p.id)
    } yield {
      (p, a)
    }

  //  val q11 =
  //    query {
  //      for {
  //        (pp, aa) <- personAndAddress
  //        s <- Queryable[Street] if (aa.streetId == s.id)
  //      } yield {
  //        (pp, aa, s.city)
  //      }
  //    }
  //  db.run(q11)
}
