package io.getquill.sources.jdbc.mysql

import io.getquill._
import io.getquill.sources.sql.EncodingSpec

class JdbcEncodingSpec extends EncodingSpec {

  "encodes and decodes types" in {
    testMysqlDB.run(delete)
    testMysqlDB.run(insert)(insertValues)
    verify(testMysqlDB.run(query[EncodingTestEntity]))
  }
}
