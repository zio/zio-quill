package io.getquill.sources.async.postgres

import io.getquill._
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.{ global => ec }

class PostgresAsyncSourceSpec extends Spec {

  def await[T](f: Future[T]) = Await.result(f, Duration.Inf)

  "run non-batched action" - {
    val insert = quote { (i: Int) =>
      qr1.insert(_.i -> i)
    }
    await(testPostgresDB.run(insert)(1)).rowsAffected mustEqual (1)
  }
}
