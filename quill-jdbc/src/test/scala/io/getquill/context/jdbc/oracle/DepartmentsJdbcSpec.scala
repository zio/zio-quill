package io.getquill.context.jdbc.oracle

import io.getquill.context.sql.DepartmentsSpec
import org.scalatest.matchers.should.Matchers._

class DepartmentsJdbcSpec extends DepartmentsSpec {

  val context = testContext
  import testContext._

  override def beforeAll = {
    testContext.transaction {
      testContext.run(infix"alter session set current_schema=quill_test".as[Update[Unit]])
      testContext.run(query[Department].delete)
      testContext.run(query[Employee].delete)
      testContext.run(query[Task].delete)

      testContext.run(liftQuery(departmentEntries).foreach(p => departmentInsert(p)))
      testContext.run(liftQuery(employeeEntries).foreach(p => employeeInsert(p)))
      testContext.run(liftQuery(taskEntries).foreach(p => taskInsert(p)))
    }
    ()
  }

  "Example 8 - nested naive" in {
    testContext.run(`Example 8 expertise naive`(lift(`Example 8 param`))) should contain theSameElementsAs `Example 8 expected result`
  }

  "Example 9 - nested db" in {
    testContext.run(`Example 9 expertise`(lift(`Example 9 param`))) should contain theSameElementsAs `Example 9 expected result`
  }
}
