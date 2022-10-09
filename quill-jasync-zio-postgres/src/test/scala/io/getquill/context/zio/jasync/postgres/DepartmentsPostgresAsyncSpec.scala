package io.getquill.context.zio.jasync.postgres

import io.getquill.context.sql.base.DepartmentsSpec

class DepartmentsPostgresAsyncSpec extends DepartmentsSpec with ZioSpec {

  import context._

  override def beforeAll =
    runSyncUnsafe {
      context.transaction {
        for {
          _ <- context.run(query[Department].delete)
          _ <- context.run(query[Employee].delete)
          _ <- context.run(query[Task].delete)

          _ <- context.run(liftQuery(departmentEntries).foreach(e => departmentInsert(e)))
          _ <- context.run(liftQuery(employeeEntries).foreach(e => employeeInsert(e)))
          _ <- context.run(liftQuery(taskEntries).foreach(e => taskInsert(e)))
        } yield {}
      }
    }

  "Example 8 - nested naive" in {
    runSyncUnsafe(testContext.run(`Example 8 expertise naive`(lift(`Example 8 param`)))) mustEqual `Example 8 expected result`
  }

  "Example 9 - nested db" in {
    runSyncUnsafe(testContext.run(`Example 9 expertise`(lift(`Example 9 param`)))) mustEqual `Example 9 expected result`
  }

  "performIO" in {
    runSyncUnsafe(performIO(runIO(query[Task]).transactional))
  }
}
