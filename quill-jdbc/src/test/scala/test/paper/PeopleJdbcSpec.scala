package test.paper

import io.getquill.impl.Source
import test.Spec
import io.getquill._
import io.getquill.jdbc.PooledJdbcSource

class PeopleJdbcSpec extends PeopleSpec {

  object peopleDB extends PooledJdbcSource

  "Example 1 - differences" in {
    peopleDB.transaction { _.run(`Ex 1 differences`) } mustEqual `Ex 1 expected result`
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
