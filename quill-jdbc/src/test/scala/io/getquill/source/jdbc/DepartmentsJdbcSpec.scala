package io.getquill.source.jdbc

import io.getquill._
import io.getquill.source.sql.DepartmentsSpec

class DepartmentsJdbcSpec extends DepartmentsSpec {

  override def beforeAll = {
    val t = testDB.transaction {
      testDB.run(queryable[Department].delete)
      testDB.run(queryable[Employee].delete)
      testDB.run(queryable[Task].delete)

      testDB.run(departmentInsert).using(departmentEntries)
      testDB.run(employeeInsert).using(employeeEntries)
      testDB.run(taskInsert).using(taskEntries)
    }
  }

  "Example 8 - nested naive" in {
    testDB.run(`Example 8 expertise naive`).using(`Example 8 param`) mustEqual `Example 8 expected result`
  }

  "Example 9 - nested db" in {
    testDB.run(`Example 9 expertise`).using(`Example 9 param`) mustEqual `Example 9 expected result`
  }
}
