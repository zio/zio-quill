package io.getquill.sources.jdbc.h2

import io.getquill._
import io.getquill.sources.sql.DepartmentsSpec

class DepartmentsJdbcSpec extends DepartmentsSpec {

  override def beforeAll = {
    val t = testH2DB.transaction { transactional =>
      transactional.run(query[Department].delete)
      transactional.run(query[Employee].delete)
      transactional.run(query[Task].delete)

      transactional.run(departmentInsert)(departmentEntries)
      transactional.run(employeeInsert)(employeeEntries)
      transactional.run(taskInsert)(taskEntries)
    }
  }

  "Example 8 - nested naive" in {
    testH2DB.run(`Example 8 expertise naive`)(`Example 8 param`) mustEqual `Example 8 expected result`
  }

  "Example 9 - nested db" in {
    testH2DB.run(`Example 9 expertise`)(`Example 9 param`) mustEqual `Example 9 expected result`
  }
}
