package io.getquill.source.async.mysql

import io.getquill._
import io.getquill.source.sql.EncodingSpec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class MysqlAsyncEncodingSpec extends EncodingSpec {

  "encodes and decodes types" in {
    val r =
      for {
        _ <- testMysqlDB.run(delete)
        _ <- testMysqlDB.run(insert).using(insertValues)
        result <- testMysqlDB.run(query[EncodingTestEntity])
      } yield result

    verify(Await.result(r, Duration.Inf).toList)
  }

  "fails if the column has the wrong type" in {
    Await.result(testMysqlDB.run(insert).using(insertValues), Duration.Inf)
    case class EncodingTestEntity(v1: Int)
    val e = intercept[IllegalStateException] {
      Await.result(testMysqlDB.run(query[EncodingTestEntity]), Duration.Inf)
    }
  }
}
