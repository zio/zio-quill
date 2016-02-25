package io.getquill.sources.finagle.mysql

import com.twitter.finagle.exp.mysql._
import com.twitter.util._
import io.getquill._

class MysqlAsyncSourceSpec extends Spec {

  def await[T](f: Future[T]) = Await.result(f)

  "run non-batched action" - {
    val insert = quote { (i: Int) =>
      qr1.insert(_.i -> i)
    }
    await(testDB.run(insert)(1)) mustBe an[OK]
  }
}
