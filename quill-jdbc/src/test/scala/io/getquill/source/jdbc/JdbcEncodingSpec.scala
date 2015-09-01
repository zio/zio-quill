package io.getquill.source.jdbc

import io.getquill.queryable
import io.getquill.quote
import io.getquill.source.sql.EncodingSpec

class JdbcEncodingSpec extends EncodingSpec {

  "encodes and decodes types" in {
    testDB.run(delete)
    testDB.run(insert).using(List(insertValues))
    verify(testDB.run(queryable[EncodingTestEntity]))
  }
}
