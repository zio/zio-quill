package io.getquill.context.jdbc.oracle

import io.getquill.context.sql.PeopleSpec

class PeopleJdbcSpec extends PeopleSpec {

  val context = testContext
  import testContext._

  override def beforeAll = {
    testContext.transaction {
      testContext.run(infix"alter session set current_schema=quill_test".as[Update[Unit]])
      testContext.run(query[Couple].delete)
      testContext.run(query[Person].filter(_.age > 0).delete)
      testContext.run(liftQuery(peopleEntries).foreach(p => peopleInsert(p)))
      testContext.run(liftQuery(couplesEntries).foreach(p => couplesInsert(p)))
    }
    ()
  }

  "Example 1 - differences" in {
    testContext.run(`Ex 1 differences`) mustEqual `Ex 1 expected result`
  }

  "Example 2 - range simple" in {
    testContext.run(`Ex 2 rangeSimple`(lift(`Ex 2 param 1`), lift(`Ex 2 param 2`))) mustEqual `Ex 2 expected result`
  }

  "Example 3 - satisfies" in {
    testContext.run(`Ex 3 satisfies`) mustEqual `Ex 3 expected result`
  }

  "Example 4 - satisfies" in {
    testContext.run(`Ex 4 satisfies`) mustEqual `Ex 4 expected result`
  }

  "Example 5 - compose" in {
    testContext.run(`Ex 5 compose`(lift(`Ex 5 param 1`), lift(`Ex 5 param 2`))) mustEqual `Ex 5 expected result`
  }

  "Example 6 - predicate 0" in {
    testContext.run(satisfies(eval(`Ex 6 predicate`))) mustEqual `Ex 6 expected result`
  }

  "Example 7 - predicate 1" in {
    testContext.run(satisfies(eval(`Ex 7 predicate`))) mustEqual `Ex 7 expected result`
  }

  "Example 8 - contains empty" in {
    testContext.run(`Ex 8 and 9 contains`(liftQuery(`Ex 8 param`))) mustEqual `Ex 8 expected result`
  }

  "Example 9 - contains non empty" in {
    testContext.run(`Ex 8 and 9 contains`(liftQuery(`Ex 9 param`))) mustEqual `Ex 9 expected result`
  }

  "Example 10 - pagination" in {
    testContext.run(`Ex 10 page 1 query`) mustEqual `Ex 10 page 1 expected`
    testContext.run(`Ex 10 page 2 query`) mustEqual `Ex 10 page 2 expected`
  }
}
