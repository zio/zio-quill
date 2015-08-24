package test.paper

import test.Spec
import io.getquill._
import test.testDB
import com.twitter.util.Future
import com.twitter.util.Await

class DepartmentsFinagleMysqlSpec extends DepartmentsSpec {

  def await[T](future: Future[T]) = Await.result(future)

  override def beforeAll =
    testDB.transaction {
      for {
        _ <- testDB.run(queryable[Department].delete)
        _ <- testDB.run(queryable[Employee].delete)
        _ <- testDB.run(queryable[Task].delete)

        _ <- testDB.run(departmentInsert)(departmentEntries)
        _ <- testDB.run(employeeInsert)(employeeEntries)
        _ <- testDB.run(taskInsert)(taskEntries)
      } yield {}
    }

  "Example 8 - nested naive" in {
    await(testDB.run(`Example 8 expertise naive`)(`Example 8 param`)) mustEqual `Example 8 expected result`
  }

  "Example 9 - nested db" in {
    await(testDB.run(`Example 9 expertise`)(`Example 9 param`)) mustEqual `Example 9 expected result`
  }
}
