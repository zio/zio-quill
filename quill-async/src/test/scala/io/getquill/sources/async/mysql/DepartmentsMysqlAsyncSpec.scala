package io.getquill.sources.async.mysql

import io.getquill._
import io.getquill.sources.sql.DepartmentsSpec
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.{ global => ec }

class DepartmentsMysqlAsyncSpec extends DepartmentsSpec {

  def await[T](future: Future[T]) = Await.result(future, Duration.Inf)

  override def beforeAll =
    await {
      testMysqlDB.transaction { transactional =>
        for {
          _ <- transactional.run(query[Department].delete)
          _ <- transactional.run(query[Employee].delete)
          _ <- transactional.run(query[Task].delete)

          _ <- transactional.run(departmentInsert)(departmentEntries)
          _ <- transactional.run(employeeInsert)(employeeEntries)
          _ <- transactional.run(taskInsert)(taskEntries)
        } yield {}
      }
    }

  "Example 8 - nested naive" in {
    await(testMysqlDB.run(`Example 8 expertise naive`)(`Example 8 param`)) mustEqual `Example 8 expected result`
  }

  "Example 9 - nested db" in {
    await(testMysqlDB.run(`Example 9 expertise`)(`Example 9 param`)) mustEqual `Example 9 expected result`
  }
}
