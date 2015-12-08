package io.getquill.source.jdbc.mysql

import io.getquill._
import io.getquill.source.sql.DepartmentsSpec

class DepartmentsJdbcSpec extends DepartmentsSpec {

  override def beforeAll = {
    val t = testMysqlDB.transaction {
      testMysqlDB.run(query[Department].delete)
      testMysqlDB.run(query[Employee].delete)
      testMysqlDB.run(query[Task].delete)

      testMysqlDB.run(departmentInsert).using(departmentEntries)
      testMysqlDB.run(employeeInsert).using(employeeEntries)
      testMysqlDB.run(taskInsert).using(taskEntries)
    }
  }

  "Example 8 - nested naive" in {
    testMysqlDB.run(`Example 8 expertise naive`).using(`Example 8 param`) mustEqual `Example 8 expected result`
  }

  "Example 9 - nested db" in {
    testMysqlDB.run(`Example 9 expertise`).using(`Example 9 param`) mustEqual `Example 9 expected result`
  }
}
