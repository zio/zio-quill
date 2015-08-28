package io.getquill.source.jdbc

import io.getquill._
import io.getquill.source.sql.EncodingSpec;

class JdbcEncodingSpec extends EncodingSpec {

  "encodes and decodes types" in {
    testDB.run(delete)
    testDB.run(insert).using(List(insertValues))
    testDB.run(queryable[EncodingTestEntity]) mustEqual List(instance)
  }
}
