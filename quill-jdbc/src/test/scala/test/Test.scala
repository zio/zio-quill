package test

import io.getquill._
import io.getquill.jdbc.JdbcSource

case class Person(id: Long, name: String, surname: String, age: Int)
case class Address(id: Long, personId: Long, streetId: Long, number: Int)
case class Street(id: Long, name: String, city: String)

object Test extends App {

  object db extends JdbcSource

  val name = quote("test")

  def q1 =
    quote {
      queryable[Person].filter(p => p.name == name).map(p => (p.name, p.age))
    }
  db.run(q1)

  def q2 =
    quote {
      for {
        p <- queryable[Person] if (p.name == null)
        a <- queryable[Address] if (a.personId == p.id)
      } yield {
        a
      }
    }

  db.run(q2)

  def q3 =
    quote {
      for {
        a <- q2
        s <- queryable[Street] if (a.streetId == s.id)
      } yield {
        a
      }
    }
  db.run(q3)

  val byName = quote {
    (name: String) => queryable[Person].filter(_.name == name)
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
    queryable[Person].filter(nameEqualsSurname(_))
  }
  db.run(q6)

  val nameIs = quote {
    (p: Person, name: String) => p.name == name
  }

  val q7 = quote {
    queryable[Person].filter(nameIs(_, "flavio"))
  }
  db.run(q7)

  val names = quote {
    queryable[Person].map(_.name)
  }

  val q8 = quote {
    for {
      name <- names
      p <- queryable[Person] if (p.name == name)
    } yield {
      (p.name, p.age)
    }
  }
  db.run(q8)

  val q9 = quote {
    queryable[Address].map(_.personId)
  }
  db.run(q9)

  val q10 =
    quote {
      for {
        p1 <- queryable[Person]
        p2 <- queryable[Person] if (p1.name == p2.name)
      } yield {
        p2
      }
    }
  db.run(q10)

  val nameAndAge =
    quote {
      for {
        p <- queryable[Person]
      } yield {
        (p.name, p.age)
      }
    }

  val q11 =
    quote {
      nameAndAge.map(_._1)
    }

  db.run(q11)

  val personAndAddress =
    quote {
      for {
        p <- queryable[Person]
        a <- queryable[Address] if (a.personId == p.id)
      } yield {
        (p, a)
      }
    }

  val q12 =
    quote {
      for {
        (pp, aa) <- personAndAddress
        s <- queryable[Street] if (aa.streetId == s.id)
      } yield {
        s
      }
    }
  db.run(q12)

}
