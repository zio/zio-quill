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

    Await.result(r) mustEqual List(instance)
  }
}
