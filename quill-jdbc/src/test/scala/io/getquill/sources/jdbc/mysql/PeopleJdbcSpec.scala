package io.getquill.sources.jdbc.mysql

import io.getquill._
import io.getquill.sources.sql.PeopleSpec
import io.getquill.quotation.Quoted
import io.getquill.quotation.IsDynamic

class PeopleJdbcSpec extends PeopleSpec {

  override def beforeAll = {
    val t = testMysqlDB.transaction {
      testMysqlDB.run(query[Couple].delete)
      testMysqlDB.run(query[Person].filter(_.age > 0).delete)
      testMysqlDB.run(peopleInsert)(peopleEntries)
      testMysqlDB.run(couplesInsert)(couplesEntries)
    }
  }

  "Example 1 - differences" in {
    testMysqlDB.run(`Ex 1 differences`) mustEqual `Ex 1 expected result`
  }

  "Example 2 - range simple" in {
    testMysqlDB.run(`Ex 2 rangeSimple`)(`Ex 2 param 1`, `Ex 2 param 2`) mustEqual `Ex 2 expected result`
  }

  "Example 3 - satisfies" in {
    testMysqlDB.run(`Ex 3 satisfies`) mustEqual `Ex 3 expected result`
  }

  "Example 4 - satisfies" in {
    testMysqlDB.run(`Ex 4 satisfies`) mustEqual `Ex 4 expected result`
  }

  "Example 5 - compose" in {
    testMysqlDB.run(`Ex 5 compose`)(`Ex 5 param 1`, `Ex 5 param 2`) mustEqual `Ex 5 expected result`
  }

  "Example 6 - predicate 0" in {
    testMysqlDB.run(satisfies(eval(`Ex 6 predicate`))) mustEqual `Ex 6 expected result`
  }

  "Example 7 - predicate 1" in {
    testMysqlDB.run(satisfies(eval(`Ex 7 predicate`))) mustEqual `Ex 7 expected result`
  }
}
