package io.getquill.sources.jdbc.postgres

import io.getquill._
import io.getquill.sources.sql.EncodingSpec

class JdbcEncodingSpec extends EncodingSpec {

  "encodes and decodes types" in {
    testPostgresDB.run(delete)
    testPostgresDB.run(insert)(insertValues)
    verify(testPostgresDB.run(query[EncodingTestEntity]))
  }
}
