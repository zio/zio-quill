package io.getquill.context.finagle.mysql

import com.twitter.util._
import io.getquill._

class MysqlAsyncSourceSpec extends Spec {

val context = testContext
  import testContext._
  
  def await[T](f: Future[T]) = Await.result(f)

  "run non-batched action" in {
    val insert = quote { (i: Int) =>
      qr1.insert(_.i -> i)
    }
    await(testContext.run(insert)(1)) mustEqual 1
  }
}
