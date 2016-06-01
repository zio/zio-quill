package io.getquill.sources.jdbc.h2

import io.getquill.sources.sql.EncodingSpec

class JdbcEncodingSpec extends EncodingSpec(testH2DB) {

  import testH2DB._

  "encodes and decodes types" in {
    testH2DB.run(delete)
    testH2DB.run(insert)(insertValues)
    verify(testH2DB.run(query[EncodingTestEntity]))
  }
}
