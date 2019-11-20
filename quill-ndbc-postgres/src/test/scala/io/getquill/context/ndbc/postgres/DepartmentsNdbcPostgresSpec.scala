package io.getquill.context.ndbc.postgres

import io.getquill.context.sql.DepartmentsSpec

class DepartmentsNdbcPostgresSpec extends DepartmentsSpec {

  val context = testContext
  import context._

  override def beforeAll = {
    get {
      context.transaction {
        for {
          a <- context.run(query[Department].delete)
          b <- context.run(query[Employee].delete)
          _ <- context.run(query[Task].delete)

          _ <- context.run(liftQuery(departmentEntries).foreach(e => departmentInsert(e)))
          _ <- context.run(liftQuery(employeeEntries).foreach(e => employeeInsert(e)))
          _ <- context.run(liftQuery(taskEntries).foreach(e => taskInsert(e)))
        } yield {}
      }
    }
    ()
  }

  "Example 8 - nested naive" in {
    get(context.run(`Example 8 expertise naive`(lift(`Example 8 param`)))) mustEqual `Example 8 expected result`
  }

  "Example 9 - nested db" in {
    get(context.run(`Example 9 expertise`(lift(`Example 9 param`)))) mustEqual `Example 9 expected result`
  }
}