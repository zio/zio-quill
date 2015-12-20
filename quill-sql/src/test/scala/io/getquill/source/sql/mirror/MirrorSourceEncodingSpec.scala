package io.getquill.source.sql.mirror

import io.getquill._
import io.getquill.source.sql.EncodingSpec
import io.getquill.source.mirror.Row

class MirrorSourceEncodingSpec extends EncodingSpec {

  "encodes and decodes types" in {
    val rows = insertValues.map(r => Row(r.productIterator.toList: _*))
    mirrorSource.run(insert).apply(insertValues).bindList mustEqual rows
    val mirror = mirrorSource.run(query[EncodingTestEntity])
    verify(rows.map(mirror.extractor))
  }
}
