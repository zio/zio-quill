package io.getquill.source.jdbc.mysql

import io.getquill._
import io.getquill.source.sql.EncodingSpec

class JdbcEncodingSpec extends EncodingSpec {

  "encodes and decodes types" in {
    testMysqlDB.run(delete)
    testMysqlDB.run(insert).using(insertValues)
    verify(testMysqlDB.run(query[EncodingTestEntity]))
  }
}
