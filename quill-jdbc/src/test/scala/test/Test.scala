package test

import io.getquill._
import io.getquill.jdbc.JdbcSource

case class Person(id: Long, name: String, surname: String, age: Int)
case class Address(id: Long, personId: Long, streetId: Long, number: Int)
case class Street(id: Long, name: String, city: String)

object Test extends App {
  
  // all[Person].filter(_.id == 42).update(_.age -> 33)
  // all[Person].insert(Person(_, "nnn", "mmm", 33))

  object db extends JdbcSource
  
  val name = quote("test")

  def q1 =
    quote {
      from[Person].filter(p => p.name == name).map(p => (p.name, p.age))
    }
  db.run(q1)

  def q2 =
    quote {
      for {
        p <- from[Person] if (p.name == null)
        a <- from[Address] if (a.personId == p.id)
      } yield {
        a
      }
    }

  db.run(q2)

  def q3 =
    quote {
      for {
        a <- q2
        s <- from[Street] if (a.streetId == s.id)
      } yield {
        a
      }
    }
  db.run(q3)

  val byName = quote {
    (name: String) => from[Person].filter(_.name == name)
  }

  val q4 =
    quote {
      byName("jesus")
    }
  db.run(q4)

  val byFullName = quote {
    (name: String, surname: String) => byName(name).filter(_.surname == surname)
  }

  val q5 = quote {
    byFullName("flavio", "brasil")
  }
  db.run(q5)

  val nameEqualsSurname = quote {
    (p: Person) => p.name == p.surname
  }

  val q6 = quote {
    from[Person].filter(nameEqualsSurname(_))
  }
  db.run(q6)

  val nameIs = quote {
    (p: Person, name: String) => p.name == name
  }

  val q7 = quote {
    from[Person].filter(nameIs(_, "flavio"))
  }
  db.run(q7)

  val names = quote {
    from[Person].map(_.name)
  }

  val q8 = quote {
    for {
      name <- names
      p <- from[Person] if (p.name == name)
    } yield {
      (p.name, p.age)
    }
  }
  db.run(q8)

  val q9 = quote {
    from[Address].map(_.personId)
  }
  db.run(q9)

  val q10 =
    quote {
      for {
        p1 <- from[Person]
        p2 <- from[Person] if (p1.name == p2.name)
      } yield {
        p2
      }
    }
  db.run(q10)

  val personAndAddress =
    quote {
      for {
        p <- from[Person]
        a <- from[Address] if (a.personId == p.id)
      } yield {
        (p, a)
      }
    }

  val q11 =
    quote {
      for {
        (pp, aa) <- personAndAddress
        s <- from[Street] if (aa.streetId == s.id)
      } yield {
        (pp, aa, s.city)
      }
    }
//  db.run(q11)
}
