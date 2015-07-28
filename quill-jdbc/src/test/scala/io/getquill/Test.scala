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
    query {
      from[Person].filter(p => p.name == p.surname).map(p => (p.name, p.age))
    }
  db.run(q1)

  def q2 =
    query {
      for {
        p <- from[Person] if (p.name == null)
        a <- from[Address] if (a.personId == p.id)
      } yield {
        a
      }
    }

  db.run(q2)

  def q3 =
    query {
      for {
        a <- q2
        s <- from[Street] if (a.streetId == s.id)
      } yield {
        a
      }
    }
  db.run(q3)

  val byName = partial {
    (name: String) => from[Person].filter(_.name == name)
  }

  val q4 =
    query {
      byName("jesus")
    }
  db.run(q4)

  val byFullName = partial {
    (name: String, surname: String) => byName(name).filter(_.surname == surname)
  }

  val q5 = query {
    byFullName("flavio", "brasil")
  }
  db.run(q5)

  val nameEqualsSurname = partial {
    (p: Person) => p.name == p.surname
  }

  val q6 = query {
    from[Person].filter(nameEqualsSurname(_))
  }
  db.run(q6)

  val nameIs = partial {
    (p: Person, name: String) => p.name == name
  }

  val q7 = query {
    from[Person].filter(nameIs(_, "flavio"))
  }
  db.run(q7)

  val names = query {
    from[Person].map(_.name)
  }

  val q8 = query {
    for {
      name <- names
      p <- from[Person] if (p.name == name)
    } yield {
      (p.name, p.age)
    }
  }
  db.run(q8)

  val q9 = query {
    from[Address].map(_.personId)
  }
  db.run(q9)

  val q10 =
    query {
      for {
        p1 <- from[Person]
        p2 <- from[Person] if (p1.name == p2.name)
      } yield {
        p2
      }
    }
  db.run(q10)

  val personAndAddress =
    query {
      for {
        p <- from[Person]
        a <- from[Address] if (a.personId == p.id)
      } yield {
        (p, a)
      }
    }

//  val q11 =
//    query {
//      for {
//        (pp, aa) <- personAndAddress
//        s <- from[Street] if (aa.streetId == s.id)
//      } yield {
//        (pp, aa, s.city)
//      }
//    }
//  db.run(q11)
}
