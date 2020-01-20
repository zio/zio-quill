package io.getquill.context.finagle.mysql

import com.twitter.util.Await
import com.twitter.util.Future

import io.getquill.context.sql.DepartmentsSpec

class DepartmentsFinagleMysqlSpec extends DepartmentsSpec {

  val context = testContext
  import testContext._

  def await[T](future: Future[T]) = Await.result(future)

  override def beforeAll =
    await {
      testContext.transaction {
        for {
          _ <- testContext.run(query[Department].delete)
          _ <- testContext.run(query[Employee].delete)
          _ <- testContext.run(query[Task].delete)

          _ <- testContext.run(liftQuery(departmentEntries).foreach(e => departmentInsert(e)))
          _ <- testContext.run(liftQuery(employeeEntries).foreach(e => employeeInsert(e)))
          _ <- testContext.run(liftQuery(taskEntries).foreach(e => taskInsert(e)))
        } yield {}
      }
    }

  "Example 8 - nested naive" in {
    await(testContext.run(`Example 8 expertise naive`(lift(`Example 8 param`)))) mustEqual `Example 8 expected result`
  }

  "Example 9 - nested db" in {
    await(testContext.run(`Example 9 expertise`(lift(`Example 9 param`)))) mustEqual `Example 9 expected result`
  }

  "Example 9 - streamed result" in {
    await(testContext.stream(`Example 9 expertise`(lift(`Example 9 param`))).flatMap(_.toSeq())) mustEqual `Example 9 expected result`
  }
}
