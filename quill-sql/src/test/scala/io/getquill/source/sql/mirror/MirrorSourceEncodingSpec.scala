package io.getquill.source.sql.mirror

import io.getquill._
import io.getquill.source.sql.EncodingSpec
import io.getquill.source.mirror.Row

class MirrorSourceEncodingSpec extends EncodingSpec {

  "encodes and decodes types" in {
    val row = Row(insertValues.productIterator.toList: _*)
    mirrorSource.run(insert).using(List(insertValues)).bindList mustEqual List(row)
    verify(List(mirrorSource.run(queryable[EncodingTestEntity]).extractor(row)))
  }
}
