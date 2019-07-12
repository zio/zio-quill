package io.getquill.context.sql.mirror

import io.getquill.context.mirror.Row
import io.getquill.context.sql.{ EncodingSpec, testContext }

class MirrorContextEncodingSpec extends EncodingSpec {

  val context = testContext

  import testContext._

  "encodes and decodes types" in {
    val rows = insertValues.map(v =>
      Row(v.v1, v.v2, v.v3, v.v4, v.v5, v.v6, v.v7, v.v8, v.v9, v.v10, v.v11, v.v12.value, v.v13, v.v14,
        v.o1, v.o2, v.o3, v.o4, v.o5, v.o6, v.o7, v.o8, v.o9, v.o10, v.o11, v.o12.map(_.value), v.o13, v.o14, v.o15.map(_.value)))
      .toList
    context.run(liftQuery(insertValues).foreach(p => insert(p))).groups.flatMap(_._2) mustEqual rows

    val mirror = context.run(query[EncodingTestEntity])
    verify(rows.map(mirror.extractor))
  }
}
