package io.getquill.sources.jdbc.h2

import io.getquill._
import io.getquill.sources.sql.EncodingSpec

class JdbcEncodingSpec extends EncodingSpec {

  "encodes and decodes types" in {
    testH2DB.run(delete)
    testH2DB.run(insert)(insertValues)
    verify(testH2DB.run(query[EncodingTestEntity]))
  }
}
