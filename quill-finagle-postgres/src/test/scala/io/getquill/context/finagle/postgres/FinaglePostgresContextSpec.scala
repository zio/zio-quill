package io.getquill.context.finagle.postgres

import com.twitter.util._
import io.getquill._

class FinaglePostgresContextSpec extends Spec {

  val context = testContext
  import testContext._

  def await[T](f: Future[T]) = Await.result(f)

  "run non-batched action" in {
    val insert = quote { (i: Int) =>
      qr1.insert(_.i -> i)
    }
    await(context.run(insert(lift(1)))) mustEqual 1
  }
}
