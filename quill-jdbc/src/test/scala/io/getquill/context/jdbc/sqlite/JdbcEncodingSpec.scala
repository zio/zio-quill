package io.getquill.context.jdbc.sqlite

import io.getquill.context.sql.EncodingSpec

class JdbcEncodingSpec extends EncodingSpec {

  val context = testContext
  import testContext._

  "encodes and decodes types" in {
    testContext.run(delete)
    testContext.run(liftQuery(insertValues).foreach(e => insert(e)))
    verify(testContext.run(query[EncodingTestEntity]))
    ()
  }
}
