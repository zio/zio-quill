package io.getquill.context.sql

import io.getquill.Spec

trait PeopleSpec extends Spec {

  val context: SqlContext[_, _]

  import context._

  case class Person(name: String, age: Int)
  case class Couple(her: String, him: String)

  val peopleInsert =
    quote((p: Person) => query[Person].insert(p))

  val peopleEntries = List(
    Person("Alex", 60),
    Person("Bert", 55),
    Person("Cora", 33),
    Person("Drew", 31),
    Person("Edna", 21),
    Person("Fred", 60)
  )

  val couplesInsert =
    quote((c: Couple) => query[Couple].insert(c))

  val couplesEntries = List(
    Couple("Alex", "Bert"),
    Couple("Cora", "Drew"),
    Couple("Edna", "Fred")
  )

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

  val `Ex 8 and 9 contains` =
    quote {
      (set: Query[Int]) =>
        query[Person].filter(p => set.contains(p.age))
    }

  val `Ex 8 param` = Set.empty[Int]
  val `Ex 8 expected result` = List.empty[Person]

  val `Ex 9 param` = Set(55, 33)
  val `Ex 9 expected result` = List(Person("Bert", 55), Person("Cora", 33))

  val `Ex 10 page 1 query` = quote {
    query[Person].sortBy(p => p.name)(Ord.asc).drop(0).take(3)
  }
  val `Ex 10 page 1 expected` = peopleEntries.sortBy(_.name).slice(0, 3)
  val `Ex 10 page 2 query` = quote {
    query[Person].sortBy(p => p.name)(Ord.asc).drop(3).take(3)
  }
  val `Ex 10 page 2 expected` = peopleEntries.sortBy(_.name).slice(3, 6)
}
