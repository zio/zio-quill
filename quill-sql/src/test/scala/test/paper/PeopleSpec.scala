package test.paper

import io.getquill.impl.Source
import test.Spec
import io.getquill._

case class Person(name: String, age: Int)
case class Couple(her: String, him: String)

trait PeopleSpec extends Spec {

  val peopleInsert =
    quote(table[Person].map(p => (p.name, p.age)))

  val peopleEntries = List(
    ("Alex", 60),
    ("Bert", 55),
    ("Cora", 33),
    ("Drew", 31),
    ("Edna", 21),
    ("Fred", 60))

  val couplesInsert =
    quote(table[Couple].map(p => (p.her, p.him)))

  val couplesEntries = List(
    ("Alex", "Bert"),
    ("Cora", "Drew"),
    ("Edna", "Fred"))

  val `Ex 1 differences` =
    quote {
      for {
        c <- table[Couple]
        w <- table[Person]
        m <- table[Person] if (c.her == w.name && c.him == m.name && w.age > m.age)
      } yield {
        (w.name, w.age - m.age)
      }
    }
  val `Ex 1 expected result` = List(("Alex", 5), ("Cora", 2))

  val `Ex 2 rangeSimple` = quote {
    (a: Int, b: Int) =>
      for {
        u <- table[Person] if (a <= u.age && u.age < b)
      } yield {
        u
      }
  }
  val `Ex 2 param 1` = 30
  val `Ex 2 param 2` = 40
  val `Ex 2 expected result` = List(Person("Cora", 33), Person("Drew", 31))

  val `Ex 3, 4` =
    quote {
      (p: Int => Boolean) =>
        for {
          u <- table[Person] if (p(u.age))
        } yield {
          u
        }
    }
  val `Ex 3 satisfies` = quote(`Ex 3, 4`((x: Int) => 20 <= x && x < 30))
  val `Ex 3 expected result` = List(Person("Edna", 21))

  val `Ex 4 satisfies` = quote(`Ex 3, 4`((x: Int) => x % 2 == 0))
  val `Ex 4 expected result` = List(Person("Alex", 60), Person("Fred", 60))

  val `Ex 5 compose` = {
    val range = quote {
      (a: Int, b: Int) =>
        for {
          u <- table[Person] if (a <= u.age && u.age < b)
        } yield {
          u
        }
    }
    val ageFromName = quote {
      (s: String) =>
        for {
          u <- table[Person] if (s == u.name)
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
  val `Ex 5 param 1` = "Drew"
  val `Ex 5 param 2` = "Bert"
  val `Ex 5 expected result` = List(Person("Cora", 33), Person("Drew", 31))
}
