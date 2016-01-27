package io.getquill.sources.async.postgres

import io.getquill._
import io.getquill.sources.sql.DepartmentsSpec
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.{global => ec}

class DepartmentsPostgresAsyncSpec extends DepartmentsSpec {

  def await[T](future: Future[T]) = Await.result(future, Duration.Inf)

  override def beforeAll =
    await {
      testPostgresDB.transaction { implicit ec =>
        for {
          _ <- testPostgresDB.run(query[Department].delete)
          _ <- testPostgresDB.run(query[Employee].delete)
          _ <- testPostgresDB.run(query[Task].delete)

          _ <- testPostgresDB.run(departmentInsert)(departmentEntries)
          _ <- testPostgresDB.run(employeeInsert)(employeeEntries)
          _ <- testPostgresDB.run(taskInsert)(taskEntries)
        } yield {}
      }
    }

  "Example 8 - nested naive" in {
    await(testPostgresDB.run(`Example 8 expertise naive`)(`Example 8 param`)) mustEqual `Example 8 expected result`
  }

  "Example 9 - nested db" in {
    await(testPostgresDB.run(`Example 9 expertise`)(`Example 9 param`)) mustEqual `Example 9 expected result`
  }
}
