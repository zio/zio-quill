package io.getquill.context.jasync.postgres

import io.getquill.context.sql.base.CaseClassQuerySpec
import scala.concurrent.ExecutionContext.Implicits.{global => ec}
import org.scalatest.matchers.should.Matchers._

class CaseClassQueryAsyncSpec extends CaseClassQuerySpec {

  val context = testContext
  import testContext._

  override def beforeAll =
    await {
      testContext.transaction { implicit ec =>
        for {
          _ <- testContext.run(query[Contact].delete)
          _ <- testContext.run(query[Address].delete)
          _ <- testContext.run(liftQuery(peopleEntries).foreach(e => peopleInsert(e)))
          _ <- testContext.run(liftQuery(addressEntries).foreach(e => addressInsert(e)))
        } yield {}
      }
    }

  "Example 1 - Single Case Class Mapping" in {
    await(
      testContext.run(`Ex 1 CaseClass Record Output`)
    ) should contain theSameElementsAs `Ex 1 CaseClass Record Output expected result`
  }
  "Example 1A - Single Case Class Mapping" in {
    await(
      testContext.run(`Ex 1A CaseClass Record Output`)
    ) should contain theSameElementsAs `Ex 1 CaseClass Record Output expected result`
  }
  "Example 1B - Single Case Class Mapping" in {
    await(
      testContext.run(`Ex 1B CaseClass Record Output`)
    ) should contain theSameElementsAs `Ex 1 CaseClass Record Output expected result`
  }

  "Example 2 - Single Record Mapped Join" in {
    await(
      testContext.run(`Ex 2 Single-Record Join`)
    ) should contain theSameElementsAs `Ex 2 Single-Record Join expected result`
  }

  "Example 3 - Inline Record as Filter" in {
    await(
      testContext.run(`Ex 3 Inline Record Usage`)
    ) should contain theSameElementsAs `Ex 3 Inline Record Usage expected result`
  }
}
