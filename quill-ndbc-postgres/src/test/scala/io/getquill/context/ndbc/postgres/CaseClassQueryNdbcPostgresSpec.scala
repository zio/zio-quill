package io.getquill.context.ndbc.postgres

import io.getquill.context.sql.base.CaseClassQuerySpec

class CaseClassQueryNdbcPostgresSpec extends CaseClassQuerySpec {

  val context = testContext
  import context._

  override def beforeAll =
    get {
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
    get(
      context.run(`Ex 1 CaseClass Record Output`)
    ) must contain theSameElementsAs (`Ex 1 CaseClass Record Output expected result`)
  }

  "Example 1A - Single Case Class Mapping" in {
    get(
      context.run(`Ex 1A CaseClass Record Output`)
    ) must contain theSameElementsAs `Ex 1 CaseClass Record Output expected result`
  }

  "Example 1B - Single Case Class Mapping" in {
    get(
      context.run(`Ex 1B CaseClass Record Output`)
    ) must contain theSameElementsAs `Ex 1 CaseClass Record Output expected result`
  }

  "Example 2 - Single Record Mapped Join" in {
    get(context.run(`Ex 2 Single-Record Join`)) must contain theSameElementsAs `Ex 2 Single-Record Join expected result`
  }

  "Example 3 - Inline Record as Filter" in {
    get(
      context.run(`Ex 3 Inline Record Usage`)
    ) must contain theSameElementsAs `Ex 3 Inline Record Usage expected result`
  }
}
