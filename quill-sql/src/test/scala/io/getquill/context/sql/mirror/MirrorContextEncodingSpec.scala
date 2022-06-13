package io.getquill.context.sql.mirror

import io.getquill.context.mirror.{ MirrorSession, Row }
import io.getquill.context.sql.{ EncodingSpec, testContext }

class MirrorContextEncodingSpec extends EncodingSpec {

  val context = testContext

  import testContext._

  implicit class OptionExt[T](t: Option[T]) {
    def getN: Any =
      t match {
        case Some(v) => v
        case None    => null
      }
  }

  "encodes and decodes types" in {
    // Since Row encoding in the mirror context is made to mirror JDBC e.g. decoding Person(name:Option[String]) expects
    // it to be Row("Joe") then we need to get rid of the optional value
    val prepareRows = insertValues.map(v =>
      Row(
        v.v1, v.v2, v.v3, v.v4, v.v5, v.v6, v.v7,
        v.v8, v.v9, v.v10, v.v11, v.v12.value, v.v13, v.v14,
        v.o1.getN, v.o2.getN, v.o3.getN, v.o4.getN, v.o5.getN, v.o6.getN, v.o7.getN,
        v.o8.getN, v.o9.getN, v.o10.getN, v.o11.getN, v.o12.map(_.value).getN, v.o13.getN, v.o14.getN,
        v.o15.map(_.value).getN
      )).toList

    // On the other hand, the row objects that are returned from the query should indeed have the name:Option[String] column
    // as Option("Joe")
    val resultRows = insertValues.map(v =>
      Row(v.v1, v.v2, v.v3, v.v4, v.v5, v.v6, v.v7, v.v8, v.v9, v.v10, v.v11, v.v12.value, v.v13, v.v14,
        v.o1, v.o2, v.o3, v.o4, v.o5, v.o6, v.o7, v.o8, v.o9, v.o10, v.o11, v.o12.map(_.value), v.o13, v.o14, v.o15.map(_.value)))
      .toList

    context.run(liftQuery(insertValues).foreach(p => insert(p))).groups.flatMap(_._2) mustEqual resultRows

    val mirror = context.run(query[EncodingTestEntity])
    verify(prepareRows.map(row => mirror.extractor(row, MirrorSession.default)))
  }
}
