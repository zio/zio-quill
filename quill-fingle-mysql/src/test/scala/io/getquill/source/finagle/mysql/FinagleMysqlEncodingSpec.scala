package io.getquill.source.finagle.mysql

import com.twitter.util.Await

import io.getquill.queryable
import io.getquill.quote
import io.getquill.source.sql.EncodingSpec

class FinagleMysqlEncodingSpec extends EncodingSpec {

  "encodes and decodes types" in {
    val r =
      for {
        _ <- testDB.run(delete)
        _ <- testDB.run(insert).using(List(insertValues))
        result <- testDB.run(queryable[EncodingTestEntity])
      } yield result

    verify(Await.result(r).toList)
  }

  "fails if the column has the wrong type" in {
    Await.result(testDB.run(insert).using(List(insertValues)))
    case class EncodingTestEntity(v1: Int)
    val e = intercept[IllegalStateException] {
      Await.result(testDB.run(queryable[EncodingTestEntity]))
    }
  }
}
