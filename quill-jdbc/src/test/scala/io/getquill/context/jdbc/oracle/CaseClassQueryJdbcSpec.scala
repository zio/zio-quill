package io.getquill.context.jdbc.oracle

import org.scalatest.matchers.should.Matchers._
import io.getquill.Update
import io.getquill.context.sql.base.CaseClassQuerySpec

class CaseClassQueryJdbcSpec extends CaseClassQuerySpec {

  val context = testContext
  import testContext._

  override def beforeAll = {
    testContext.transaction {
      testContext.run(sql"alter session set current_schema=quill_test".as[Update[Unit]])
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

  "Example 4 - Ex 4 Mapped Union of Nicknames" in {
    println(translate(`Ex 4 Mapped Union of Nicknames`))
    testContext.run(
      `Ex 4 Mapped Union of Nicknames`
    ) should contain theSameElementsAs `Ex 4 Mapped Union of Nicknames expected result`
  }

  "Example 4 - Ex 4 Mapped Union All of Nicknames" in {
    testContext.run(
      `Ex 4 Mapped Union All of Nicknames`
    ) should contain theSameElementsAs `Ex 4 Mapped Union All of Nicknames expected result`
  }

  "Example 4 - Ex 4 Mapped Union All of Nicknames Filtered" in {
    testContext.run(
      `Ex 4 Mapped Union All of Nicknames Filtered`
    ) should contain theSameElementsAs `Ex 4 Mapped Union All of Nicknames Filtered expected result`
  }

  "Example 4 - Ex 4 Mapped Union All of Nicknames Same Field" in {
    testContext.run(
      `Ex 4 Mapped Union All of Nicknames Same Field`
    ) should contain theSameElementsAs `Ex 4 Mapped Union All of Nicknames Same Field expected result`
  }

  "Example 4 - Ex 4 Mapped Union All of Nicknames Same Field Filtered" in {
    testContext.run(
      `Ex 4 Mapped Union All of Nicknames Same Field Filtered`
    ) should contain theSameElementsAs `Ex 4 Mapped Union All of Nicknames Same Field Filtered expected result`
  }
}
