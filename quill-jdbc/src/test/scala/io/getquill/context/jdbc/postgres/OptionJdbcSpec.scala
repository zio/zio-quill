package io.getquill.context.jdbc.postgres

import io.getquill.context.sql.OptionQuerySpec
import org.scalatest.matchers.should.Matchers._

class OptionJdbcSpec extends OptionQuerySpec {

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

  "Example 1 - Simple Map with Condition" in {
    testContext.run(`Simple Map with Condition`) should contain theSameElementsAs `Simple Map with Condition Result`
  }

  "Example 1.1 - Simple Map with GetOrElse" in {
    testContext.run(`Simple Map with GetOrElse`) should contain theSameElementsAs `Simple Map with GetOrElse Result`
  }

  "Example 1.2 - Simple Map with Condition and GetOrElse" in {
    testContext.run(`Simple Map with Condition and GetOrElse`) should contain theSameElementsAs `Simple Map with Condition and GetOrElse Result`
  }

  "Example 2 - Simple GetOrElse" in {
    testContext.run(`Simple GetOrElse`) should contain theSameElementsAs `Simple GetOrElse Result`
  }

  "Example 3 - LeftJoin with FlatMap" in {
    testContext.run(`LeftJoin with FlatMap`) should contain theSameElementsAs `LeftJoin with FlatMap Result`
  }

  "Example 4 - LeftJoin with Flatten" in {
    testContext.run(`LeftJoin with Flatten`) should contain theSameElementsAs `LeftJoin with Flatten Result`
  }

  "Example 5 - Map+getOrElse Join" in {
    testContext.run(`Map+getOrElse LeftJoin`) should contain theSameElementsAs `Map+getOrElse LeftJoin Result`
  }

  "Example 6 - Map+Option+Flatten+getOrElse Join" in {
    testContext.run(`Option+Some+None Normalize`) should contain theSameElementsAs `Option+Some+None Normalize Result`
  }
}
