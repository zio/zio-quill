package io.getquill.context.jdbc.h2

import io.getquill.context.sql.EncodingSpec

import java.time.ZoneId

class JdbcEncodingSpec extends EncodingSpec {

  val context = testContext
  import testContext._

  "encodes and decodes types" in {
    testContext.run(delete)
    testContext.run(liftQuery(insertValues).foreach(p => insert(p)))
    verify(testContext.run(query[EncodingTestEntity]))
  }

  "Encode/Decode Other Time Types" in {
    context.run(query[TimeEntity].delete)
    val zid        = ZoneId.systemDefault()
    val timeEntity = TimeEntity.make(zid)
    context.run(query[TimeEntity].insertValue(lift(timeEntity)))
    val actual = context.run(query[TimeEntity]).head
    timeEntity mustEqual actual
  }
}
