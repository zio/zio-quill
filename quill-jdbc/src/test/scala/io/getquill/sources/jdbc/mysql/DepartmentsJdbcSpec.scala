package io.getquill.sources.jdbc.mysql

import io.getquill._
import io.getquill.sources.sql.DepartmentsSpec

class DepartmentsJdbcSpec extends DepartmentsSpec {

  override def beforeAll = {
    val t = testMysqlDB.transaction {
      testMysqlDB.run(query[Department].delete)
      testMysqlDB.run(query[Employee].delete)
      testMysqlDB.run(query[Task].delete)

      testMysqlDB.run(departmentInsert)(departmentEntries)
      testMysqlDB.run(employeeInsert)(employeeEntries)
      testMysqlDB.run(taskInsert)(taskEntries)
    }
  }

  "Example 8 - nested naive" in {
    testMysqlDB.run(`Example 8 expertise naive`)(`Example 8 param`) mustEqual `Example 8 expected result`
  }

  "Example 9 - nested db" in {
    testMysqlDB.run(`Example 9 expertise`)(`Example 9 param`) mustEqual `Example 9 expected result`
  }
}
