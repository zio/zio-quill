package io.getquill.context.sql.mirror

import io.getquill.context.sql.mirrorContext._
import io.getquill.context.sql.EncodingSpec
import io.getquill.context.mirror.Row
import io.getquill.context.sql.mirrorContext

class MirrorContextEncodingSpec extends EncodingSpec(mirrorContext) {

  "encodes and decodes types" in {
    val rows = insertValues.map(r => Row(r.productIterator.toList: _*))
    mirrorContext.run(insert).apply(insertValues).bindList mustEqual rows
    val mirror = mirrorContext.run(query[EncodingTestEntity])
    verify(rows.map(mirror.extractor))
  }
}
