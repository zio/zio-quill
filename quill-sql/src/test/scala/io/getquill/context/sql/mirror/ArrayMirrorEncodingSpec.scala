package io.getquill.context.sql.mirror

import java.time.LocalDate
import java.util.Date

import io.getquill.context.sql.encoding.ArrayEncodingBaseSpec
import io.getquill.context.sql.testContext

class ArrayMirrorEncodingSpec extends ArrayEncodingBaseSpec {
  val ctx = testContext

  import ctx._

  val q = quote(query[ArraysTestEntity])

  "Support all sql base types and `Seq` implementers" in {
    val insertStr = ctx.run(q.insert(lift(e))).string
    val selectStr = ctx.run(q).string

    insertStr mustEqual "INSERT INTO ArraysTestEntity (texts,decimals,bools,bytes,shorts,ints,longs,floats," +
      "doubles,timestamps,dates) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"

    selectStr mustEqual "SELECT x.texts, x.decimals, x.bools, x.bytes, x.shorts, x.ints, x.longs, x.floats, " +
      "x.doubles, x.timestamps, x.dates FROM ArraysTestEntity x"
  }

  "Support Seq encoding basing on MappedEncoding" in {
    val wrapQ = quote(querySchema[WrapEntity]("ArraysTestEntity"))

    val insertStr = ctx.run(wrapQ.insert(lift(wrapE))).string
    val selectStr = ctx.run(wrapQ).string
    insertStr mustEqual "INSERT INTO ArraysTestEntity (texts) VALUES (?)"
    selectStr mustEqual "SELECT x.texts FROM ArraysTestEntity x"
  }

  "Provide implicit encoders for raw types" in {
    implicitly[Encoder[List[String]]]
    implicitly[Encoder[List[BigDecimal]]]
    implicitly[Encoder[List[Boolean]]]
    implicitly[Encoder[List[Byte]]]
    implicitly[Encoder[List[Short]]]
    implicitly[Encoder[List[Index]]]
    implicitly[Encoder[List[Long]]]
    implicitly[Encoder[List[Float]]]
    implicitly[Encoder[List[Double]]]
    implicitly[Encoder[List[Date]]]
    implicitly[Encoder[List[LocalDate]]]
  }

  "Provide implicit decoders for raw types" in {
    implicitly[Decoder[List[String]]]
    implicitly[Decoder[List[BigDecimal]]]
    implicitly[Decoder[List[Boolean]]]
    implicitly[Decoder[List[Byte]]]
    implicitly[Decoder[List[Short]]]
    implicitly[Decoder[List[Index]]]
    implicitly[Decoder[List[Long]]]
    implicitly[Decoder[List[Float]]]
    implicitly[Decoder[List[Double]]]
    implicitly[Decoder[List[Date]]]
    implicitly[Decoder[List[LocalDate]]]
  }
}
