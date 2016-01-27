package io.getquill.sources.finagle.mysql

import com.twitter.util.Await

import io.getquill._
import io.getquill.sources.sql.EncodingSpec

class FinagleMysqlEncodingSpec extends EncodingSpec {

  "encodes and decodes types" in {
    val r =
      for {
        _ <- testDB.run(delete)
        _ <- testDB.run(insert)(insertValues)
        result <- testDB.run(query[EncodingTestEntity])
      } yield result

    verify(Await.result(r).toList)
  }

  "fails if the column has the wrong type" in {
    Await.result(testDB.run(insert)(insertValues))
    case class EncodingTestEntity(v1: Int)
    val e = intercept[IllegalStateException] {
      Await.result(testDB.run(query[EncodingTestEntity]))
    }
  }
}
