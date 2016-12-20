package io.getquill.sources.async.mysqlio

import io.getquill._
import io.getquill.sources.sql.PeopleSpec
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class PeopleMysqlAsyncSpec extends PeopleSpec {

  def await[T](future: Future[T]) = Await.result(future, Duration.Inf)

  override def beforeAll =
    await {

      val dbActions = for {
        _ <- testMysqlIO.run(query[Couple].delete)
        _ <- testMysqlIO.run(query[Person].filter(_.age > 0).delete)
        _ <- testMysqlIO.run(peopleInsert)(peopleEntries)
        _ <- testMysqlIO.run(couplesInsert)(couplesEntries)
      } yield {}
      dbActions.unsafePerformTrans
    }

  "Example 1 - differences" in {
    await(testMysqlIO.run(`Ex 1 differences`).unsafePerformIO) mustEqual `Ex 1 expected result`
  }

  "Example 2 - range simple" in {
    await(testMysqlIO.run(`Ex 2 rangeSimple`)(`Ex 2 param 1`, `Ex 2 param 2`).unsafePerformIO) mustEqual `Ex 2 expected result`
  }

  "Examples 3 - satisfies" in {
    await(testMysqlIO.run(`Ex 3 satisfies`).unsafePerformIO) mustEqual `Ex 3 expected result`
  }

  "Examples 4 - satisfies" in {
    await(testMysqlIO.run(`Ex 4 satisfies`).unsafePerformIO) mustEqual `Ex 4 expected result`
  }

  "Example 5 - compose" in {
    await(testMysqlIO.run(`Ex 5 compose`)(`Ex 5 param 1`, `Ex 5 param 2`).unsafePerformIO) mustEqual `Ex 5 expected result`
  }

  "Example 6 - predicate 0" in {
    await(testMysqlIO.run(satisfies(eval(`Ex 6 predicate`))).unsafePerformIO) mustEqual `Ex 6 expected result`
  }

  "Example 7 - predicate 1" in {
    await(testMysqlIO.run(satisfies(eval(`Ex 7 predicate`))).unsafePerformIO) mustEqual `Ex 7 expected result`
  }

  "Example 8 - contains empty" in {
    await(testMysqlIO.run(`Ex 8 and 9 contains`)(`Ex 8 param`).unsafePerformIO) mustEqual `Ex 8 expected result`
  }

  "Example 9 - contains non empty" in {
    await(testMysqlIO.run(`Ex 8 and 9 contains`)(`Ex 9 param`).unsafePerformIO) mustEqual `Ex 9 expected result`
  }
}
