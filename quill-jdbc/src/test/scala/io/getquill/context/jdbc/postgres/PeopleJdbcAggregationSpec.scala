package io.getquill.context.jdbc.postgres

import io.getquill.context.sql.PeopleAggregationSpec

class PeopleJdbcAggregationSpec extends PeopleAggregationSpec {

  val context = testContext
  import testContext._

  override def beforeAll = {
    testContext.transaction {
      testContext.run(query[Contact].delete)
      testContext.run(query[Address].delete)
      testContext.run(liftQuery(people).foreach(p => query[Contact].insertValue(p)))
      testContext.run(liftQuery(addresses).foreach(a => query[Address].insertValue(a)))
    }
    ()
  }

  "Ex 1 map(agg(c),agg(c))" in {
    import `Ex 1 map(agg(c),agg(c))`._
    context.run(get) mustEqual expect
  }

  "Ex 2 map(agg(c),agg(c)).filter(col)" in {
    import `Ex 2 map(agg(c),agg(c)).filter(col)`._
    context.run(get) mustEqual expect
  }

  "Ex 3 groupByMap(col)(col,agg(c))" in {
    import `Ex 3 groupByMap(col)(col,agg(c))`._
    context.run(get).toSet mustEqual expect.toSet
  }

  "Ex 4 groupByMap(col)(agg(c)).filter(agg)" in {
    import `Ex 4 groupByMap(col)(agg(c)).filter(agg)`._
    context.run(get).toSet mustEqual expect.toSet
  }

  "Ex 5 map.groupByMap(col)(col,agg(c)).filter(agg)" in {
    import `Ex 5 map.groupByMap(col)(col,agg(c)).filter(agg)`._
    context.run(get).toSet mustEqual expect.toSet
  }

  "Ex 6 flatMap.groupByMap.map" in {
    import `Ex 6 flatMap.groupByMap.map`._
    context.run(get).toSet mustEqual expect.toSet
  }
}
