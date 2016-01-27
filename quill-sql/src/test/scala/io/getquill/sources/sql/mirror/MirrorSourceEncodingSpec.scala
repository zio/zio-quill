package io.getquill.sources.sql.mirror

import io.getquill._
import io.getquill.sources.sql.EncodingSpec
import io.getquill.sources.mirror.Row
import io.getquill.sources.sql.mirrorSource

class MirrorSourceEncodingSpec extends EncodingSpec {

  "encodes and decodes types" in {
    val rows = insertValues.map(r => Row(r.productIterator.toList: _*))
    mirrorSource.run(insert).apply(insertValues).bindList mustEqual rows
    val mirror = mirrorSource.run(query[EncodingTestEntity])
    verify(rows.map(mirror.extractor))
  }
}
