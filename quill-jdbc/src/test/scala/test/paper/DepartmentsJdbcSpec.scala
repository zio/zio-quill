package test.paper

import io.getquill.queryable
import io.getquill.unquote
import test.testDB
import test.testDB.run

class DepartmentsJdbcSpec extends DepartmentsSpec {

  override def beforeAll =
    testDB.transaction {
      testDB.run(queryable[Department].delete)
      testDB.run(queryable[Employee].delete)
      testDB.run(queryable[Task].delete)

      testDB.run(departmentInsert)(departmentEntries)
      testDB.run(employeeInsert)(employeeEntries)
      testDB.run(taskInsert)(taskEntries)
    }

  "Example 8 - nested naive" in {
    testDB.run(`Example 8 expertise naive`)(`Example 8 param`) mustEqual `Example 8 expected result`
  }

  "Example 9 - nested db" in {
    testDB.run(`Example 9 expertise`)(`Example 9 param`) mustEqual `Example 9 expected result`
  }
}
