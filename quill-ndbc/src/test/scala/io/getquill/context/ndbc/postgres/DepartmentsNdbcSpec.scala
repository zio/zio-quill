package io.getquill.context.ndbc.postgres

import io.getquill.context.sql.DepartmentsSpec

import io.getquill.context.sql.DepartmentsSpec
import io.trane.future.scala.Future
import io.trane.future.scala.Await
import scala.concurrent.duration._
import language.postfixOps

class DepartmentsNdbcSpec extends DepartmentsSpec {

  val context = testContext
  import testContext._

  def await[T](future: Future[T]) = Await.result(future, 9999 seconds)

  override def beforeAll = {
    await {
      testContext.transaction {
        for {
          a <- testContext.run(query[Department].delete)
          b <- testContext.run(query[Employee].delete)
          _ <- testContext.run(query[Task].delete)

          _ <- testContext.run(liftQuery(departmentEntries).foreach(e => departmentInsert(e)))
          _ <- testContext.run(liftQuery(employeeEntries).foreach(e => employeeInsert(e)))
          _ <- testContext.run(liftQuery(taskEntries).foreach(e => taskInsert(e)))
        } yield {}
      }
    }
    ()
  }

  "Example 8 - nested naive" in {
    await(testContext.run(`Example 8 expertise naive`(lift(`Example 8 param`)))) mustEqual `Example 8 expected result`
  }

  "Example 9 - nested db" in {
    await(testContext.run(`Example 9 expertise`(lift(`Example 9 param`)))) mustEqual `Example 9 expected result`
  }
}