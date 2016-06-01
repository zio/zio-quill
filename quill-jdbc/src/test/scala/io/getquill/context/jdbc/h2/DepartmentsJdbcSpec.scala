package io.getquill.context.jdbc.h2

import io.getquill.context.sql.DepartmentsSpec

class DepartmentsJdbcSpec extends DepartmentsSpec {

  val context = testContext
  import testContext._

  override def beforeAll = {
    testContext.transaction {
      testContext.run(query[Department].delete)
      testContext.run(query[Employee].delete)
      testContext.run(query[Task].delete)

      testContext.run(departmentInsert)(departmentEntries)
      testContext.run(employeeInsert)(employeeEntries)
      testContext.run(taskInsert)(taskEntries)
    }
    ()
  }

  "Example 8 - nested naive" in {
    testContext.run(`Example 8 expertise naive`)(`Example 8 param`) mustEqual `Example 8 expected result`
  }

  "Example 9 - nested db" in {
    testContext.run(`Example 9 expertise`)(`Example 9 param`) mustEqual `Example 9 expected result`
  }
}
