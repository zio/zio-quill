package io.getquill.context.async.mysql

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

import io.getquill.Spec

class MysqlAsyncContextSpec extends Spec {

  import testContext._

  def await[T](f: Future[T]) = Await.result(f, Duration.Inf)

  "run non-batched action" in {
    val insert = quote { (i: Int) =>
      qr1.insert(_.i -> i)
    }
    await(testContext.run(insert)(List(1))) mustEqual (List(1))
  }
}
