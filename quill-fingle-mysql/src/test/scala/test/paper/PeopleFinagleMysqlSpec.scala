package test.paper

import io.getquill.impl.Source
import test.Spec
import io.getquill._
import com.twitter.util.Await
import com.twitter.util.Future
import io.getquill.finagle.mysql.FinagleMysqlSource

class PeopleFinagleMysqlSpec extends PeopleSpec {

  object peopleDB extends FinagleMysqlSource

  def await[T](future: Future[T]) = Await.result(future)

  "Example 1 - differences" in {
    await(peopleDB.transaction(peopleDB.query(`Ex 1 differences`))) mustEqual `Ex 1 expected result`
  }

  "Example 2 - range simple" in {
    await(peopleDB.query(`Ex 2 rangeSimple`)(`Ex 2 param 1`, `Ex 2 param 2`)) mustEqual `Ex 2 expected result`
  }

  "Examples 3 - satisfies" in {
    await(peopleDB.query(`Ex 3 satisfies`)) mustEqual `Ex 3 expected result`
  }

  "Examples 4 - satisfies" in {
    await(peopleDB.query(`Ex 4 satisfies`)) mustEqual `Ex 4 expected result`
  }

  "Example 5 - compose" in {
    await(peopleDB.query(`Ex 5 compose`)(`Ex 5 param 1`, `Ex 5 param 2`)) mustEqual `Ex 5 expected result`
  }

}
