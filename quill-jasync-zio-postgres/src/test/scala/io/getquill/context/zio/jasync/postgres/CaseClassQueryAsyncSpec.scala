package io.getquill.context.zio.jasync.postgres

import io.getquill.context.sql.base.CaseClassQuerySpec
import org.scalatest.matchers.should.Matchers._

class CaseClassQueryAsyncSpec extends CaseClassQuerySpec with ZioSpec {

  import context._

  override def beforeAll =
    runSyncUnsafe {
      context.transaction {
        for {
          _ <- context.run(query[Contact].delete)
          _ <- context.run(query[Address].delete)
          _ <- context.run(liftQuery(peopleEntries).foreach(e => peopleInsert(e)))
          _ <- context.run(liftQuery(addressEntries).foreach(e => addressInsert(e)))
        } yield {}
      }
    }

  "Example 1 - Single Case Class Mapping" in {
    runSyncUnsafe(
      context.run(`Ex 1 CaseClass Record Output`)
    ) should contain theSameElementsAs `Ex 1 CaseClass Record Output expected result`
  }
  "Example 1A - Single Case Class Mapping" in {
    runSyncUnsafe(
      context.run(`Ex 1A CaseClass Record Output`)
    ) should contain theSameElementsAs `Ex 1 CaseClass Record Output expected result`
  }
  "Example 1B - Single Case Class Mapping" in {
    runSyncUnsafe(
      context.run(`Ex 1B CaseClass Record Output`)
    ) should contain theSameElementsAs `Ex 1 CaseClass Record Output expected result`
  }

  "Example 2 - Single Record Mapped Join" in {
    runSyncUnsafe(
      context.run(`Ex 2 Single-Record Join`)
    ) should contain theSameElementsAs `Ex 2 Single-Record Join expected result`
  }

  "Example 3 - Inline Record as Filter" in {
    runSyncUnsafe(
      context.run(`Ex 3 Inline Record Usage`)
    ) should contain theSameElementsAs `Ex 3 Inline Record Usage expected result`
  }
}
