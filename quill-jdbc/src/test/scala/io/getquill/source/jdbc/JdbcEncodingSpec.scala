package io.getquill.source.jdbc

import io.getquill._
import io.getquill.source.sql.EncodingSpec

class JdbcEncodingSpec extends EncodingSpec {

  "encodes and decodes types" in {
    testDB.run(delete)
    testDB.run(insert).using(insertValues)
    verify(testDB.run(query[EncodingTestEntity]))
  }
}
