package io.getquill.sources.finagle.mysql

import com.twitter.util.Await
import com.twitter.util.Future

import io.getquill._
import io.getquill.sources.sql.DepartmentsSpec

class DepartmentsFinagleMysqlSpec extends DepartmentsSpec {

  def await[T](future: Future[T]) = Await.result(future)

  override def beforeAll =
    await {
      testDB.transaction { transactional =>
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
    await(testDB.run(`Example 8 expertise naive`)(`Example 8 param`)) mustEqual `Example 8 expected result`
  }

  "Example 9 - nested db" in {
    await(testDB.run(`Example 9 expertise`)(`Example 9 param`)) mustEqual `Example 9 expected result`
  }
}
