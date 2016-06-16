package io.getquill.sources.async.mysql

import io.getquill._
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.{ global => ec }

class MysqlAsyncSourceSpec extends Spec {

  def await[T](f: Future[T]) = Await.result(f, Duration.Inf)

  "run non-batched action" - {
    val insert = quote { (i: Int) =>
      qr1.insert(_.i -> i)
    }
    await(testMysqlDB.run(insert)(1)) mustEqual (1)
  }
}
