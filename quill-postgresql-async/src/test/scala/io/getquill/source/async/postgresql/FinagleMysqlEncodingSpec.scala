package io.getquill.source.async.postgresql

import io.getquill._
import io.getquill.source.sql.EncodingSpec
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

class FinagleMysqlEncodingSpec extends EncodingSpec {

  "encodes and decodes types" in {
    val r =
      for {
        _ <- testDB.run(delete)
        _ <- testDB.run(insert).using(insertValues)
        result <- testDB.run(query[EncodingTestEntity])
      } yield result

    verify(Await.result(r, Duration.Inf).toList)
  }

  "fails if the column has the wrong type" in {
    Await.result(testDB.run(insert).using(insertValues), Duration.Inf)
    case class EncodingTestEntity(v1: Int)
    val e = intercept[IllegalStateException] {
      Await.result(testDB.run(query[EncodingTestEntity]), Duration.Inf)
    }
  }
}
