package io.getquill.sources.async.mysqlio

import io.getquill._
import io.getquill.sources.sql.DepartmentsSpec
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class DepartmentsMysqlAsyncSpec extends DepartmentsSpec {

  def await[T](future: Future[T]) = Await.result(future, Duration.Inf)

  override def beforeAll =
    await {
      val dbAction = for {
        _ <- testMysqlIO.run(query[Department].delete)
        _ <- testMysqlIO.run(query[Employee].delete)
        _ <- testMysqlIO.run(query[Task].delete)

        _ <- testMysqlIO.run(departmentInsert)(departmentEntries)
        _ <- testMysqlIO.run(employeeInsert)(employeeEntries)
        _ <- testMysqlIO.run(taskInsert)(taskEntries)
      } yield {}
      dbAction.unsafePerformTrans
    }

  "Example 8 - nested naive" in {
    await(testMysqlIO.run(`Example 8 expertise naive`)(`Example 8 param`).unsafePerformIO) mustEqual `Example 8 expected result`
  }

  "Example 9 - nested db" in {
    await(testMysqlIO.run(`Example 9 expertise`)(`Example 9 param`).unsafePerformIO) mustEqual `Example 9 expected result`
  }
}
