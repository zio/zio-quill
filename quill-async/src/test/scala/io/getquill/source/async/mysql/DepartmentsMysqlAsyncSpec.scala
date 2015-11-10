package io.getquill.source.async.mysql

import io.getquill._
import io.getquill.source.sql.DepartmentsSpec
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.{global => ec}

class DepartmentsMysqlAsyncSpec extends DepartmentsSpec {

  def await[T](future: Future[T]) = Await.result(future, Duration.Inf)
  
  override def beforeAll =
    await {
      testMysqlDB.transaction { implicit ec =>
        for {
          _ <- testMysqlDB.run(query[Department].delete)
          _ <- testMysqlDB.run(query[Employee].delete)
          _ <- testMysqlDB.run(query[Task].delete)

          _ <- testMysqlDB.run(departmentInsert).using(departmentEntries)
          _ <- testMysqlDB.run(employeeInsert).using(employeeEntries)
          _ <- testMysqlDB.run(taskInsert).using(taskEntries)
        } yield {}
      }
    }

  "Example 8 - nested naive" in {
    await(testMysqlDB.run(`Example 8 expertise naive`).using(`Example 8 param`)) mustEqual `Example 8 expected result`
  }

  "Example 9 - nested db" in {
    await(testMysqlDB.run(`Example 9 expertise`).using(`Example 9 param`)) mustEqual `Example 9 expected result`
  }
}
