package io.getquill.sqlite

import io.getquill.PeopleMonixSpec
import org.scalatest.matchers.should.Matchers._

class PeopleMonixJdbcSpec extends PeopleMonixSpec {

  val context = testContext
  import testContext._

  override def beforeAll = {
    testContext.transaction {
      for {
        _ <- testContext.run(query[Couple].delete)
        _ <- testContext.run(query[Person].filter(_.age > 0).delete)
        _ <- testContext.run(liftQuery(peopleEntries).foreach(p => peopleInsert(p)))
        _ <- testContext.run(liftQuery(couplesEntries).foreach(p => couplesInsert(p)))
      } yield ()
    }.runSyncUnsafe()
  }

  "Example 1 - differences" in {
    testContext.run(`Ex 1 differences`).runSyncUnsafe() should contain theSameElementsAs `Ex 1 expected result`
  }

  "Example 2 - range simple" in {
    testContext.run(`Ex 2 rangeSimple`(lift(`Ex 2 param 1`), lift(`Ex 2 param 2`))).runSyncUnsafe() should contain theSameElementsAs `Ex 2 expected result`
  }

  "Example 3 - satisfies" in {
    testContext.run(`Ex 3 satisfies`).runSyncUnsafe() should contain theSameElementsAs `Ex 3 expected result`
  }

  "Example 4 - satisfies" in {
    testContext.run(`Ex 4 satisfies`).runSyncUnsafe() should contain theSameElementsAs `Ex 4 expected result`
  }

  "Example 5 - compose" in {
    testContext.run(`Ex 5 compose`(lift(`Ex 5 param 1`), lift(`Ex 5 param 2`))).runSyncUnsafe() mustEqual `Ex 5 expected result`
  }

  "Example 6 - predicate 0" in {
    testContext.run(satisfies(eval(`Ex 6 predicate`))).runSyncUnsafe() mustEqual `Ex 6 expected result`
  }

  "Example 7 - predicate 1" in {
    testContext.run(satisfies(eval(`Ex 7 predicate`))).runSyncUnsafe() mustEqual `Ex 7 expected result`
  }

  "Example 8 - contains empty" in {
    testContext.run(`Ex 8 and 9 contains`(liftQuery(`Ex 8 param`))).runSyncUnsafe() mustEqual `Ex 8 expected result`
  }

  "Example 9 - contains non empty" in {
    testContext.run(`Ex 8 and 9 contains`(liftQuery(`Ex 9 param`))).runSyncUnsafe() mustEqual `Ex 9 expected result`
  }

  "Example 10 - pagination" in {
    testContext.run(`Ex 10 page 1 query`).runSyncUnsafe() mustEqual `Ex 10 page 1 expected`
    testContext.run(`Ex 10 page 2 query`).runSyncUnsafe() mustEqual `Ex 10 page 2 expected`
  }

  "Example 11 - streaming" in {
    collect(testContext.stream(`Ex 11 query`)) should contain theSameElementsAs `Ex 11 expected`
  }
}
