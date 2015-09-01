package io.getquill.source.sql

import java.util.Date

import scala.BigDecimal

import io.getquill.Spec
import io.getquill.queryable
import io.getquill.quote

class EncodingSpec extends Spec {

  case class EncodingTestEntity(
    v1: String,
    v2: BigDecimal,
    v3: Boolean,
    v4: Byte,
    v5: Short,
    v6: Int,
    v7: Long,
    v8: Float,
    v9: Double,
    v10: Array[Byte],
    v11: Date)

  val delete = quote {
    queryable[EncodingTestEntity].delete
  }

  val insert = quote {
    (v1: String,
    v2: BigDecimal,
    v3: Boolean,
    v4: Byte,
    v5: Short,
    v6: Int,
    v7: Long,
    v8: Float,
    v9: Double,
    v10: Array[Byte],
    v11: Date) =>
      queryable[EncodingTestEntity].insert(
        _.v1 -> v1,
        _.v2 -> v2,
        _.v3 -> v3,
        _.v4 -> v4,
        _.v5 -> v5,
        _.v6 -> v6,
        _.v7 -> v7,
        _.v8 -> v8,
        _.v9 -> v9,
        _.v10 -> v10,
        _.v11 -> v11)
  }

  val insertValues =
    (
      "s",
      BigDecimal(1.1),
      true,
      11.toByte,
      23.toShort,
      33,
      431L,
      34.4f,
      42d,
      Array(1.toByte, 2.toByte),
      new Date(31200000))

  def verify(result: List[EncodingTestEntity]) = {
    result.size mustEqual 1
    val entity = result.head
    entity.v1 mustEqual "s"
    entity.v2 mustEqual BigDecimal(1.1)
    entity.v3 mustEqual true
    entity.v4 mustEqual 11.toByte
    entity.v5 mustEqual 23.toShort
    entity.v6 mustEqual 33
    entity.v7 mustEqual 431L
    entity.v8 mustEqual 34.4f
    entity.v9 mustEqual 42d
    entity.v10.toList mustEqual List(1.toByte, 2.toByte)
    entity.v11 mustEqual new Date(31200000)
  }

}
