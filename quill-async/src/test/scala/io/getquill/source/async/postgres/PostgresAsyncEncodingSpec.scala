package io.getquill.source.async.postgres

import io.getquill._
import io.getquill.source.sql.EncodingSpec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.util.Date

class PostgresAsyncEncodingSpec extends EncodingSpec {

  "encodes and decodes types" in {
    val r =
      for {
        _ <- testPostgresDB.run(delete)
        _ <- testPostgresDB.run(insert).using(insertValues)
        result <- testPostgresDB.run(query[EncodingTestEntity])
      } yield result

    verify(Await.result(r, Duration.Inf).toList)
  }

  "fails if the column has the wrong type" - {
    "numeric" in {
      Await.result(testPostgresDB.run(insert).using(insertValues), Duration.Inf)
      case class EncodingTestEntity(v1: Int)
      val e = intercept[IllegalStateException] {
        Await.result(testPostgresDB.run(query[EncodingTestEntity]), Duration.Inf)
      }
    }
    "non-numeric" in {
      Await.result(testPostgresDB.run(insert).using(insertValues), Duration.Inf)
      case class EncodingTestEntity(v1: Date)
      val e = intercept[IllegalStateException] {
        Await.result(testPostgresDB.run(query[EncodingTestEntity]), Duration.Inf)
      }
    }
  }
}
