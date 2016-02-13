package io.getquill.sources.sql

import io.getquill._

trait PeopleSpec extends Spec {

  case class Person(name: String, age: Int)
  case class Couple(her: String, him: String)

  val peopleInsert =
    quote(query[Person].insert)

  val peopleEntries = List(
    Person("Alex", 60),
    Person("Bert", 55),
    Person("Cora", 33),
    Person("Drew", 31),
    Person("Edna", 21),
    Person("Fred", 60))

  val couplesInsert =
    quote(query[Couple].insert)

  val couplesEntries = List(
    Couple("Alex", "Bert"),
    Couple("Cora", "Drew"),
    Couple("Edna", "Fred"))

  val `Ex 1 differences` =
    quote {
      for {
        c <- query[Couple]
        w <- query[Person]
        m <- query[Person] if (c.her == w.name && c.him == m.name && w.age > m.age)
      } yield {
        (w.name, w.age - m.age)
      }
    }
  val `Ex 1 expected result` = List(("Alex", 5), ("Cora", 2))

  val `Ex 2 rangeSimple` = quote {
    (a: Int, b: Int) =>
      for {
        u <- query[Person] if (a <= u.age && u.age < b)
      } yield {
        u
      }
  }
  val `Ex 2 param 1` = 30
  val `Ex 2 param 2` = 40
  val `Ex 2 expected result` = List(Person("Cora", 33), Person("Drew", 31))

  val satisfies =
    quote {
      (p: Int => Boolean) =>
        for {
          u <- query[Person] if (p(u.age))
        } yield {
          u
        }
    }
  val `Ex 3 satisfies` = quote(satisfies((x: Int) => 20 <= x && x < 30))
  val `Ex 3 expected result` = List(Person("Edna", 21))

  val `Ex 4 satisfies` = quote(satisfies((x: Int) => x % 2 == 0))
  val `Ex 4 expected result` = List(Person("Alex", 60), Person("Fred", 60))

  val `Ex 5 compose` = {
    val range = quote {
      (a: Int, b: Int) =>
        for {
          u <- query[Person] if (a <= u.age && u.age < b)
        } yield {
          u
        }
    }
    val ageFromName = quote {
      (s: String) =>
        for {
          u <- query[Person] if (s == u.name)
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

  sealed trait Predicate
  case class Above(i: Int) extends Predicate
  case class Below(i: Int) extends Predicate
  case class And(a: Predicate, b: Predicate) extends Predicate
  case class Or(a: Predicate, b: Predicate) extends Predicate
  case class Not(p: Predicate) extends Predicate

  def eval(t: Predicate): Quoted[Int => Boolean] =
    t match {
      case Above(n)    => quote((x: Int) => x > lift(n))
      case Below(n)    => quote((x: Int) => x < lift(n))
      case And(t1, t2) => quote((x: Int) => eval(t1)(x) && eval(t2)(x))
      case Or(t1, t2)  => quote((x: Int) => eval(t1)(x) || eval(t2)(x))
      case Not(t0)     => quote((x: Int) => !eval(t0)(x))
    }

  val `Ex 6 predicate` = And(Above(30), Below(40))
  val `Ex 6 expected result` = List(Person("Cora", 33), Person("Drew", 31))

  val `Ex 7 predicate` = Not(Or(Below(20), Above(30)))
  val `Ex 7 expected result` = List(Person("Edna", 21))
}
