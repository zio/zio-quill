package io.getquill

import com.twitter.util.Future
import com.twitter.util.Await
import io.getquill.testDB.run

class DepartmentsFinagleMysqlSpec extends DepartmentsSpec {

  def await[T](future: Future[T]) = Await.result(future)

  override def beforeAll =
    testDB.transaction {
      for {
        _ <- testDB.run(queryable[Department].delete)
        _ <- testDB.run(queryable[Employee].delete)
        _ <- testDB.run(queryable[Task].delete)

        _ <- testDB.run(departmentInsert).using(departmentEntries)
        _ <- testDB.run(employeeInsert).using(employeeEntries)
        _ <- testDB.run(taskInsert).using(taskEntries)
      } yield {}
    }

  "Example 8 - nested naive" in {
    await(testDB.run(`Example 8 expertise naive`).using(`Example 8 param`)) mustEqual `Example 8 expected result`
  }

  "Example 9 - nested db" in {
    await(testDB.run(`Example 9 expertise`).using(`Example 9 param`)) mustEqual `Example 9 expected result`
  }
}
