package io.getquill.context.qzio.jasync.postgres

import io.getquill.context.sql.base.PeopleSpec

class PeoplePostgresAsyncSpec extends PeopleSpec with ZioSpec {

  import context._

  override def beforeAll =
    runSyncUnsafe {
      testContext.transaction {
        for {
          _ <- testContext.run(query[Couple].delete)
          _ <- testContext.run(query[Person].delete)
          _ <- testContext.run(liftQuery(peopleEntries).foreach(e => peopleInsert(e)))
          _ <- testContext.run(liftQuery(couplesEntries).foreach(e => couplesInsert(e)))
        } yield {}
      }
    }

  "Example 1 - differences" in {
    runSyncUnsafe(testContext.run(`Ex 1 differences`)) mustEqual `Ex 1 expected result`
  }

  "Example 2 - range simple" in {
    runSyncUnsafe(
      testContext.run(`Ex 2 rangeSimple`(lift(`Ex 2 param 1`), lift(`Ex 2 param 2`)))
    ) mustEqual `Ex 2 expected result`
  }

  "Examples 3 - satisfies" in {
    runSyncUnsafe(testContext.run(`Ex 3 satisfies`)) mustEqual `Ex 3 expected result`
  }

  "Examples 4 - satisfies" in {
    runSyncUnsafe(testContext.run(`Ex 4 satisfies`)) mustEqual `Ex 4 expected result`
  }

  "Example 5 - compose" in {
    runSyncUnsafe(
      testContext.run(`Ex 5 compose`(lift(`Ex 5 param 1`), lift(`Ex 5 param 2`)))
    ) mustEqual `Ex 5 expected result`
  }

  "Example 6 - predicate 0" in {
    runSyncUnsafe(testContext.run(satisfies(eval(`Ex 6 predicate`)))) mustEqual `Ex 6 expected result`
  }

  "Example 7 - predicate 1" in {
    runSyncUnsafe(testContext.run(satisfies(eval(`Ex 7 predicate`)))) mustEqual `Ex 7 expected result`
  }

  "Example 8 - contains empty" in {
    runSyncUnsafe(testContext.run(`Ex 8 and 9 contains`(liftQuery(`Ex 8 param`)))) mustEqual `Ex 8 expected result`
  }

  "Example 9 - contains non empty" in {
    runSyncUnsafe(testContext.run(`Ex 8 and 9 contains`(liftQuery(`Ex 9 param`)))) mustEqual `Ex 9 expected result`
  }
}
