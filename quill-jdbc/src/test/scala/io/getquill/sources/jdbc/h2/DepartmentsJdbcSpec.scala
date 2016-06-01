package io.getquill.sources.jdbc.h2

import io.getquill.sources.sql.DepartmentsSpec

class DepartmentsJdbcSpec extends DepartmentsSpec(testH2DB) {

  import testH2DB._

  override def beforeAll = {
    val t = testH2DB.transaction {
      testH2DB.run(query[Department].delete)
      testH2DB.run(query[Employee].delete)
      testH2DB.run(query[Task].delete)

      testH2DB.run(departmentInsert)(departmentEntries)
      testH2DB.run(employeeInsert)(employeeEntries)
      testH2DB.run(taskInsert)(taskEntries)
    }
  }

  "Example 8 - nested naive" in {
    testH2DB.run(`Example 8 expertise naive`)(`Example 8 param`) mustEqual `Example 8 expected result`
  }

  "Example 9 - nested db" in {
    testH2DB.run(`Example 9 expertise`)(`Example 9 param`) mustEqual `Example 9 expected result`
  }
}
