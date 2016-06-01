package io.getquill.sources.finagle.mysql

import com.twitter.util._
import io.getquill._

class MysqlAsyncSourceSpec extends SourceSpec(testDB) {

  import testDB._

  def await[T](f: Future[T]) = Await.result(f)

  "run non-batched action" in {
    val insert = quote { (i: Int) =>
      qr1.insert(_.i -> i)
    }
    await(testDB.run(insert)(1)) mustEqual 1
  }
}
