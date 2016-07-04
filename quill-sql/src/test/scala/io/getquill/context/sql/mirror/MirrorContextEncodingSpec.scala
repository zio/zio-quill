package io.getquill.context.sql.mirror

import io.getquill.context.mirror.Row
import io.getquill.context.sql.EncodingSpec
import io.getquill.context.sql.testContext
import io.getquill.context.sql.testContext.query
import io.getquill.context.sql.testContext.quote

class MirrorContextEncodingSpec extends EncodingSpec {

  val context = testContext

  "encodes and decodes types" in {
    val rows = insertValues.map(r => Row(r.productIterator.toList: _*))
    testContext.run(insert).apply(insertValues).bindList mustEqual rows
    val mirror = testContext.run(query[EncodingTestEntity])
    verify(rows.map(mirror.extractor))
  }
}
