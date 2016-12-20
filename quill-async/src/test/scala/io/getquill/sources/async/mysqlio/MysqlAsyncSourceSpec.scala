package io.getquill.sources.async.mysqlio

import io.getquill._
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration

class MysqlAsyncSourceSpec extends Spec {

  def await[T](f: Future[T]) = Await.result(f, Duration.Inf)

  "run non-batched action" - {
    val insert = quote { (i: Int) =>
      qr1.insert(_.i -> i)
    }
    await(testMysqlIO.run(insert)(1).unsafePerformIO) mustEqual (1)
  }
}
