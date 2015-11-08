package io.getquill.source.async.mysql

import io.getquill._
import io.getquill.source.sql.PeopleSpec
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

class PeopleMysqlAsyncSpec extends PeopleSpec {

  def await[T](future: Future[T]) = Await.result(future, Duration.Inf)

  override def beforeAll =
    await {
      testMysqlDB.transaction { implicit ec =>
        for {
          _ <- testMysqlDB.run(query[Couple].delete)
          _ <- testMysqlDB.run(query[Person].filter(_.age > 0).delete)
          _ <- testMysqlDB.run(peopleInsert).using(peopleEntries)
          _ <- testMysqlDB.run(couplesInsert).using(couplesEntries)
        } yield {}
      }
    }

  "Example 1 - differences" in {
    await(testMysqlDB.run(`Ex 1 differences`)) mustEqual `Ex 1 expected result`
  }

  "Example 2 - range simple" in {
    await(testMysqlDB.run(`Ex 2 rangeSimple`).using(`Ex 2 param 1`, `Ex 2 param 2`)) mustEqual `Ex 2 expected result`
  }

  "Examples 3 - satisfies" in {
    await(testMysqlDB.run(`Ex 3 satisfies`)) mustEqual `Ex 3 expected result`
  }

  "Examples 4 - satisfies" in {
    await(testMysqlDB.run(`Ex 4 satisfies`)) mustEqual `Ex 4 expected result`
  }

  "Example 5 - compose" in {
    await(testMysqlDB.run(`Ex 5 compose`).using(`Ex 5 param 1`, `Ex 5 param 2`)) mustEqual `Ex 5 expected result`
  }

}
