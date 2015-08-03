package test

import io.getquill._
import io.getquill.jdbc.JdbcSource

case class Person(id: Long, name: String, surname: String, age: Int)
case class Address(id: Long, personId: Long, streetId: Long, number: Int)
case class Street(id: Long, name: String, city: String)

object Test extends App {

  // peopleDB.insert(table[Person].map(name, surname, age))("nnn", "mmm", 33)
  // peopleDB.update(table[Person].filter(_.id == 42))(_.age -> 33)
  // peopleDB.upsert(table[Person])(Person(0, "nnn", "mmm", 33)

  object db extends JdbcSource

  val name = quote("test")

  def q1 =
    quote {
      table[Person].filter(p => p.name == name).map(p => (p.name, p.age))
    }
  db.query(q1)

  def q2 =
    quote {
      for {
        p <- table[Person] if (p.name == null)
        a <- table[Address] if (a.personId == p.id)
      } yield {
        a
      }
    }

  db.query(q2)

  def q3 =
    quote {
      for {
        a <- q2
        s <- table[Street] if (a.streetId == s.id)
      } yield {
        a
      }
    }
  db.query(q3)

  val byName = quote {
    (name: String) => table[Person].filter(_.name == name)
  }

  val q4 =
    quote {
      byName("jesus")
    }
  db.query(q4)

  val byFullName = quote {
    (name: String, surname: String) => byName(name).filter(_.surname == surname)
  }

  val q5 = quote {
    byFullName("flavio", "brasil")
  }
  db.query(q5)

  val nameEqualsSurname = quote {
    (p: Person) => p.name == p.surname
  }

  val q6 = quote {
    table[Person].filter(nameEqualsSurname(_))
  }
  db.query(q6)

  val nameIs = quote {
    (p: Person, name: String) => p.name == name
  }

  val q7 = quote {
    table[Person].filter(nameIs(_, "flavio"))
  }
  db.query(q7)

  val names = quote {
    table[Person].map(_.name)
  }

  val q8 = quote {
    for {
      name <- names
      p <- table[Person] if (p.name == name)
    } yield {
      (p.name, p.age)
    }
  }
  db.query(q8)

  val q9 = quote {
    table[Address].map(_.personId)
  }
  db.query(q9)

  val q10 =
    quote {
      for {
        p1 <- table[Person]
        p2 <- table[Person] if (p1.name == p2.name)
      } yield {
        p2
      }
    }
  db.query(q10)

  val personAndAddress =
    quote {
      for {
        p <- table[Person]
        a <- table[Address] if (a.personId == p.id)
      } yield {
        (p, a)
      }
    }

  val q11 =
    quote {
      for {
        (pp, aa) <- personAndAddress
        s <- table[Street] if (aa.streetId == s.id)
      } yield {
        (pp, aa, s.city)
      }
    }
  //  db.query(q11)
}
