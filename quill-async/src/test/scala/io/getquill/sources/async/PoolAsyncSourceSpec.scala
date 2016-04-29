package io.getquill.sources.async

import io.getquill._
import io.getquill.sources.async.mysql._
import io.getquill.sources.async.postgres._

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }
import scala.concurrent.ExecutionContext.Implicits.{ global => ec }

class PoolAsyncSourceSpec extends Spec {

  def await[T](f: Future[T]) = Await.result(f, Duration.Inf)

  def insert = quote { (i: Int) =>
    qr1.insert(_.i -> i)
  }

  "run non-batched action on mysql" - {
    await(testMysqlDB.run(insert)(1)) mustEqual (1)
  }

  "run non-batched action on postgres" - {
    await(testPostgresDB.run(insert)(1)) mustEqual (1)
  }
}
