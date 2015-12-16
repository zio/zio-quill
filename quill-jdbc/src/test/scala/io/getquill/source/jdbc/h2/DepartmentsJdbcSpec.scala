package io.getquill.source.jdbc.h2

import io.getquill._
import io.getquill.source.sql.DepartmentsSpec

class DepartmentsJdbcSpec extends DepartmentsSpec {

  override def beforeAll = {
    val t = testH2DB.transaction {
      testH2DB.run(query[Department].delete)
      testH2DB.run(query[Employee].delete)
      testH2DB.run(query[Task].delete)

      testH2DB.run(departmentInsert).using(departmentEntries)
      testH2DB.run(employeeInsert).using(employeeEntries)
      testH2DB.run(taskInsert).using(taskEntries)
    }
  }

  "Example 8 - nested naive" in {
    testH2DB.run(`Example 8 expertise naive`).using(`Example 8 param`) mustEqual `Example 8 expected result`
  }

  "Example 9 - nested db" in {
    testH2DB.run(`Example 9 expertise`).using(`Example 9 param`) mustEqual `Example 9 expected result`
  }
}
