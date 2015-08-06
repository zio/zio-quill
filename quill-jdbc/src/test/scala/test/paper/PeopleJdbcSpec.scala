package test.paper

import io.getquill.impl.Source
import test.Spec
import io.getquill._
import io.getquill.jdbc.JdbcSource

class PeopleJdbcSpec extends PeopleSpec {

  object peopleDB extends JdbcSource
  
//  peopleDB.run(table[Person].delete)

//  override def beforeAll =
//    peopleDB.transaction {
//      peopleDB.delete(table[Couple])
//      peopleDB.delete(table[Person].filter(_.age > 0))
//      peopleDB.insert(peopleInsert)(peopleEntries)
//      peopleDB.insert(couplesInsert)(couplesEntries)
//    }

  "Example 1 - differences" in {
    peopleDB.transaction { peopleDB.run(`Ex 1 differences`) } mustEqual `Ex 1 expected result`
  }

  "Example 2 - range simple" in {
    peopleDB.run(`Ex 2 rangeSimple`)(`Ex 2 param 1`, `Ex 2 param 2`) mustEqual `Ex 2 expected result`
  }

  "Examples 3 - satisfies" in {
    peopleDB.run(`Ex 3 satisfies`) mustEqual `Ex 3 expected result`
  }

  "Examples 4 - satisfies" in {
    peopleDB.run(`Ex 4 satisfies`) mustEqual `Ex 4 expected result`
  }

  "Example 5 - compose" in {
    peopleDB.run(`Ex 5 compose`)(`Ex 5 param 1`, `Ex 5 param 2`) mustEqual `Ex 5 expected result`
  }

}
