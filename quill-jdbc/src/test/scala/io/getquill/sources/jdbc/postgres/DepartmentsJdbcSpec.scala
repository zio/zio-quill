package io.getquill.sources.jdbc.postgres

import io.getquill._
import io.getquill.sources.sql.DepartmentsSpec

class DepartmentsJdbcSpec extends DepartmentsSpec {

  override def beforeAll = {
    val t = testPostgresDB.transaction {
      testPostgresDB.run(query[Department].delete)
      testPostgresDB.run(query[Employee].delete)
      testPostgresDB.run(query[Task].delete)

      testPostgresDB.run(departmentInsert)(departmentEntries)
      testPostgresDB.run(employeeInsert)(employeeEntries)
      testPostgresDB.run(taskInsert)(taskEntries)
    }
  }

  "Example 8 - nested naive" in {
    testPostgresDB.run(`Example 8 expertise naive`)(`Example 8 param`) mustEqual `Example 8 expected result`
  }

  "Example 9 - nested db" in {
    testPostgresDB.run(`Example 9 expertise`)(`Example 9 param`) mustEqual `Example 9 expected result`
  }
}
