package io.getquill.context.jdbc.postgres

import io.getquill.context.sql.DistinctSpec
import org.scalatest.matchers.should.Matchers._

class DistinctJdbcSpec extends DistinctSpec {

  val context = testContext
  import testContext._

  override def beforeAll = {
    testContext.transaction {
      testContext.run(query[Couple].delete)
      testContext.run(query[Person].filter(_.age > 0).delete)
      testContext.run(liftQuery(peopleEntries).foreach(p => peopleInsert(p)))
      testContext.run(liftQuery(couplesEntries).foreach(p => couplesInsert(p)))
    }
    ()
  }

  "Ex 1 Distinct One Field" in {
    testContext.run(`Ex 1 Distinct One Field`) should contain theSameElementsAs `Ex 1 Distinct One Field Result`
  }
  "Ex 2 Distinct Two Field Tuple`" in {
    testContext.run(`Ex 2 Distinct Two Field Tuple`) should contain theSameElementsAs `Ex 2 Distinct Two Field Tuple Result`
  }
  "Ex 2a Distinct Two Field Tuple Same Element`" in {
    testContext.run(`Ex 2a Distinct Two Field Tuple Same Element`) should contain theSameElementsAs `Ex 2a Distinct Two Field Tuple Same Element Result`
  }
  "Ex 3 Distinct Two Field Case Class`" in {
    testContext.run(`Ex 3 Distinct Two Field Case Class`) should contain theSameElementsAs `Ex 3 Distinct Two Field Case Class Result`
  }
  "Ex 4-base non-Distinct Subquery`" in {
    testContext.run(`Ex 4-base non-Distinct Subquery`) should contain theSameElementsAs `Ex 4-base non-Distinct Subquery Result`
  }
  "Ex 4 Distinct Subquery`" in {
    testContext.run(`Ex 4 Distinct Subquery`) should contain theSameElementsAs `Ex 4 Distinct Subquery Result`
  }
  "Ex 5 Distinct Subquery with Map Single Field" in {
    testContext.run(`Ex 5 Distinct Subquery with Map Single Field`) should contain theSameElementsAs `Ex 5 Distinct Subquery with Map Single Field Result`
  }
  "Ex 6 Distinct Subquery with Map Multi Field" in {
    testContext.run(`Ex 6 Distinct Subquery with Map Multi Field`) should contain theSameElementsAs `Ex 6 Distinct Subquery with Map Multi Field Result`
  }
  "Ex 7 Distinct Subquery with Map Multi Field Tuple" in {
    testContext.run(`Ex 7 Distinct Subquery with Map Multi Field Tuple`) should contain theSameElementsAs `Ex 7 Distinct Subquery with Map Multi Field Tuple Result`
  }
  "Ex 8 Distinct With Sort" in {
    testContext.run(`Ex 8 Distinct With Sort`) mustEqual `Ex 8 Distinct With Sort Result`
  }
}
