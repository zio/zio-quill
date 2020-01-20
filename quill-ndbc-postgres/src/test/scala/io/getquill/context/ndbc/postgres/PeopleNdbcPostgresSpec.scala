package io.getquill.context.ndbc.postgres

import io.getquill.context.sql.PeopleSpec

class PeopleNdbcPostgresSpec extends PeopleSpec {

  val context = testContext
  import context._

  override def beforeAll =
    get {
      context.transaction {
        for {
          _ <- context.run(query[Couple].delete)
          _ <- context.run(query[Person].filter(_.age > 0).delete)
          _ <- context.run(liftQuery(peopleEntries).foreach(e => peopleInsert(e)))
          _ <- context.run(liftQuery(couplesEntries).foreach(e => couplesInsert(e)))
        } yield {}
      }
    }

  "Example 1 - differences" in {
    get(context.run(`Ex 1 differences`)) mustEqual `Ex 1 expected result`
  }

  "Example 2 - range simple" in {
    get(context.run(`Ex 2 rangeSimple`(lift(`Ex 2 param 1`), lift(`Ex 2 param 2`)))) mustEqual `Ex 2 expected result`
  }

  "Example 3 - satisfies" in {
    get(context.run(`Ex 3 satisfies`)) mustEqual `Ex 3 expected result`
  }

  "Example 4 - satisfies" in {
    get(context.run(`Ex 4 satisfies`)) mustEqual `Ex 4 expected result`
  }

  "Example 5 - compose" in {
    get(context.run(`Ex 5 compose`(lift(`Ex 5 param 1`), lift(`Ex 5 param 2`)))) mustEqual `Ex 5 expected result`
  }

  "Example 6 - predicate 0" in {
    get(context.run(satisfies(eval(`Ex 6 predicate`)))) mustEqual `Ex 6 expected result`
  }

  "Example 7 - predicate 1" in {
    get(context.run(satisfies(eval(`Ex 7 predicate`)))) mustEqual `Ex 7 expected result`
  }

  "Example 8 - contains empty" in {
    get(context.run(`Ex 8 and 9 contains`(liftQuery(`Ex 8 param`)))) mustEqual `Ex 8 expected result`
  }

  "Example 9 - contains non empty" in {
    get(context.run(`Ex 8 and 9 contains`(liftQuery(`Ex 9 param`)))) mustEqual `Ex 9 expected result`
  }

  "Example 10 - pagination" in {
    get(context.run(`Ex 10 page 1 query`)) mustEqual `Ex 10 page 1 expected`
    get(context.run(`Ex 10 page 2 query`)) mustEqual `Ex 10 page 2 expected`
  }
}