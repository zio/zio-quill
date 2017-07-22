package io.getquill.context.ndbc.postgres

import io.trane.future.scala.Await
import io.trane.future.scala.Future
import scala.concurrent.duration.Duration

import io.getquill.Spec

class NdbcContextSpec extends Spec {

  import testContext._

  def await[T](f: Future[T]) = Await.result(f, Duration.Inf)

  "run non-batched action" in {
    await(testContext.run(qr1.delete))
    val insert = quote { (i: Int) =>
      qr1.insert(_.i -> i)
    }
    await(testContext.run(insert(lift(1)))) mustEqual 1
  }
}
