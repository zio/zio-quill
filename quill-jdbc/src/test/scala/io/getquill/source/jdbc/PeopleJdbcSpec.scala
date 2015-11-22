package io.getquill.source.jdbc

import io.getquill._
import io.getquill.source.sql.PeopleSpec
import io.getquill.quotation.Quoted

class PeopleJdbcSpec extends PeopleSpec {

  override def beforeAll = {
    val t = testDB.transaction {
      testDB.run(query[Couple].delete)
      testDB.run(query[Person].filter(_.age > 0).delete)
      testDB.run(peopleInsert).using(peopleEntries)
      testDB.run(couplesInsert).using(couplesEntries)
    }
  }

  "Example 1 - differences" in {
    testDB.run(`Ex 1 differences`) mustEqual `Ex 1 expected result`
  }

  "Example 2 - range simple" in {
    testDB.run(`Ex 2 rangeSimple`).using(`Ex 2 param 1`, `Ex 2 param 2`) mustEqual `Ex 2 expected result`
  }

  "Example 3 - satisfies" in {
    testDB.run(`Ex 3 satisfies`) mustEqual `Ex 3 expected result`
  }

  "Example 4 - satisfies" in {
    testDB.run(`Ex 4 satisfies`) mustEqual `Ex 4 expected result`
  }

  "Example 5 - compose" in {
    testDB.run(`Ex 5 compose`).using(`Ex 5 param 1`, `Ex 5 param 2`) mustEqual `Ex 5 expected result`
  }

  "Example 6" in {
    sealed trait Predicate
    case class Above(i: Int) extends Predicate
    case class Below(i: Int) extends Predicate
    case class And(a: Predicate, b: Predicate) extends Predicate
    case class Or(a: Predicate, b: Predicate) extends Predicate
    case class Not(p: Predicate) extends Predicate

    //    def eval(t: Predicate): Quoted[Int => Boolean] =
    //      t match {
    //        case Above(n)    => quote(x => x > 1)
    //        case Below(n)    => quote(x => x < 1)
    //        case And(t1, t2) => quote(x => eval(t1)(x) && eval(t2)(x))
    //        case Or(t1, t2)  => quote(x => eval(t1)(x) || eval(t2)(x))
    //        case Not(t0)     => quote(x => !eval(t0)(x))
    //      }
    //
    //    val q = quote((p: Predicate) => `Ex 3, 4`(eval(p)))
    //
    //    println(q.ast)
  }

}
