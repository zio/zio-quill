package io.getquill

import java.util.Date

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
    //    v10: Array[Byte],
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
    //    v10: Array[Byte],
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
        //        _.v10 -> v10,
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
      //      Array(1.toByte, 2.toByte),
      new Date(312))

  val instance =
    EncodingTestEntity(
      v1 = "s",
      v2 = BigDecimal(1.1),
      v3 = true,
      v4 = 11.toByte,
      v5 = 23.toShort,
      v6 = 33,
      v7 = 431L,
      v8 = 34.4f,
      v9 = 42d,
      //      v10 = Array(1.toByte, 2.toByte),
      v11 = new Date(312))

}
