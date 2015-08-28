package io.getquill.source.finagle.mysql

import io.getquill._
import io.getquill.source.sql.EncodingSpec
import com.twitter.util.Await

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
    intercept[IllegalStateException] {
      Await.result(testDB.run(queryable[EncodingTestEntity]))
    }
  }
}
