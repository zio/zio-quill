package io.getquill.context.async.postgres

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

import io.getquill.Spec

class PostgresAsyncContextSpec extends Spec {

  import testContext._

  def await[T](f: Future[T]) = Await.result(f, Duration.Inf)

  "run non-batched action" in {
    val insert = quote { (i: Int) =>
      qr1.insert(_.i -> i)
    }
    await(testContext.run(insert(lift(1)))) mustEqual 1
  }

  "Insert with returning with single column table" in {
    val inserted: Long = await(testContext.run {
      qr4.insert(lift(TestEntity4(0))).returning(_.i)
    })
    await(testContext.run(qr4.filter(_.i == lift(inserted))))
      .head.i mustBe inserted
  }
}
