package io.getquill.source.async.mysql

import io.getquill._
import io.getquill.source.sql.DepartmentsSpec
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

class DepartmentsMysqlAsyncSpec extends DepartmentsSpec {

  def await[T](future: Future[T]) = Await.result(future, Duration.Inf)

  override def beforeAll =
    await {
      testDB.transaction { implicit ec =>
        for {
          _ <- testDB.run(query[Department].delete)
          _ <- testDB.run(query[Employee].delete)
          _ <- testDB.run(query[Task].delete)

          _ <- testDB.run(departmentInsert).using(departmentEntries)
          _ <- testDB.run(employeeInsert).using(employeeEntries)
          _ <- testDB.run(taskInsert).using(taskEntries)
        } yield {}
      }
    }

  "Example 8 - nested naive" in {
    await(testDB.run(`Example 8 expertise naive`).using(`Example 8 param`)) mustEqual `Example 8 expected result`
  }

  "Example 9 - nested db" in {
    await(testDB.run(`Example 9 expertise`).using(`Example 9 param`)) mustEqual `Example 9 expected result`
  }
}
