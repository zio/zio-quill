package io.getquill.context.jdbc.sqlite

import io.getquill.context.sql.base.CaseClassQuerySpec
import org.scalatest.matchers.should.Matchers._

class CaseClassQueryJdbcSpec extends CaseClassQuerySpec {

  val context = testContext
  import testContext._

  override def beforeAll = {
    testContext.transaction {
      testContext.run(query[Contact].delete)
      testContext.run(query[Address].delete)
      testContext.run(liftQuery(peopleEntries).foreach(p => peopleInsert(p)))
      testContext.run(liftQuery(addressEntries).foreach(p => addressInsert(p)))
    }
    ()
  }

  "Example 1 - Single Case Class Mapping" in {
    testContext.run(
      `Ex 1 CaseClass Record Output`
    ) should contain theSameElementsAs `Ex 1 CaseClass Record Output expected result`
  }
  "Example 1A - Single Case Class Mapping" in {
    testContext.run(
      `Ex 1A CaseClass Record Output`
    ) should contain theSameElementsAs `Ex 1 CaseClass Record Output expected result`
  }
  "Example 1B - Single Case Class Mapping" in {
    testContext.run(
      `Ex 1B CaseClass Record Output`
    ) should contain theSameElementsAs `Ex 1 CaseClass Record Output expected result`
  }

  "Example 2 - Single Record Mapped Join" in {
    testContext.run(
      `Ex 2 Single-Record Join`
    ) should contain theSameElementsAs `Ex 2 Single-Record Join expected result`
  }

  "Example 3 - Inline Record as Filter" in {
    testContext.run(
      `Ex 3 Inline Record Usage`
    ) should contain theSameElementsAs `Ex 3 Inline Record Usage expected result`
  }
}
