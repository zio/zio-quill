package io.getquill.context

import java.util.Date

import scala.BigDecimal

import io.getquill.Spec

trait EncodingSpec extends Spec {

  val c: Context[_, _]

  import c._

  case class EncodingTestEntity(
    v1:  String,
    v2:  BigDecimal,
    v3:  Boolean,
    v4:  Byte,
    v5:  Short,
    v6:  Int,
    v7:  Long,
    v8:  Float,
    v9:  Double,
    v10: Array[Byte],
    v11: Date,
    o1:  Option[String],
    o2:  Option[BigDecimal],
    o3:  Option[Boolean],
    o4:  Option[Byte],
    o5:  Option[Short],
    o6:  Option[Int],
    o7:  Option[Long],
    o8:  Option[Float],
    o9:  Option[Double],
    o10: Option[Array[Byte]],
    o11: Option[Date]
  )

  val delete = quote {
    query[EncodingTestEntity].delete
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
    v11: Date,
    o1: Option[String],
    o2: Option[BigDecimal],
    o3: Option[Boolean],
    o4: Option[Byte],
    o5: Option[Short],
    o6: Option[Int],
    o7: Option[Long],
    o8: Option[Float],
    o9: Option[Double],
    o10: Option[Array[Byte]],
    o11: Option[Date]) =>
      query[EncodingTestEntity].insert(
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
        _.v11 -> v11,
        _.o1 -> o1,
        _.o2 -> o2,
        _.o3 -> o3,
        _.o4 -> o4,
        _.o5 -> o5,
        _.o6 -> o6,
        _.o7 -> o7,
        _.o8 -> o8,
        _.o9 -> o9,
        _.o10 -> o10,
        _.o11 -> o11
      )
  }

  val insertValues =
    List[(String, BigDecimal, Boolean, Byte, Short, Int, Long, Float, Double, Array[Byte], java.util.Date, Option[String], Option[BigDecimal], Option[Boolean], Option[Byte], Option[Short], Option[Int], Option[Long], Option[Float], Option[Double], Option[Array[Byte]], Option[java.util.Date])](
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
        new Date(31200000),
        Some("s"),
        Some(BigDecimal(1.1)),
        Some(true),
        Some(11.toByte),
        Some(23.toShort),
        Some(33),
        Some(431L),
        Some(34.4f),
        Some(42d),
        Some(Array(1.toByte, 2.toByte)),
        Some(new Date(31200000))
      ),
      ("",
        BigDecimal(0),
        false,
        0.toByte,
        0.toShort,
        0,
        0L,
        0F,
        0D,
        Array(),
        new Date(0),
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None
      )
    )

  def verify(result: List[EncodingTestEntity]) =
    result match {
      case List(e1, e2) =>

        e1.v1 mustEqual "s"
        e1.v2 mustEqual BigDecimal(1.1)
        e1.v3 mustEqual true
        e1.v4 mustEqual 11.toByte
        e1.v5 mustEqual 23.toShort
        e1.v6 mustEqual 33
        e1.v7 mustEqual 431L
        e1.v8 mustEqual 34.4f
        e1.v9 mustEqual 42d
        e1.v10.toList mustEqual List(1.toByte, 2.toByte)
        e1.v11 mustEqual new Date(31200000)

        e1.o1 mustEqual Some("s")
        e1.o2 mustEqual Some(BigDecimal(1.1))
        e1.o3 mustEqual Some(true)
        e1.o4 mustEqual Some(11.toByte)
        e1.o5 mustEqual Some(23.toShort)
        e1.o6 mustEqual Some(33)
        e1.o7 mustEqual Some(431L)
        e1.o8 mustEqual Some(34.4f)
        e1.o9 mustEqual Some(42d)
        e1.o10.map(_.toList) mustEqual Some(List(1.toByte, 2.toByte))
        e1.o11 mustEqual Some(new Date(31200000))

        e2.v1 mustEqual ""
        e2.v2 mustEqual BigDecimal(0)
        e2.v3 mustEqual false
        e2.v4 mustEqual 0.toByte
        e2.v5 mustEqual 0.toShort
        e2.v6 mustEqual 0
        e2.v7 mustEqual 0L
        e2.v8 mustEqual 0f
        e2.v9 mustEqual 0d
        e2.v10.toList mustEqual Nil
        e2.v11 mustEqual new Date(0)

        e2.o1 mustEqual None
        e2.o2 mustEqual None
        e2.o3 mustEqual None
        e2.o4 mustEqual None
        e2.o5 mustEqual None
        e2.o6 mustEqual None
        e2.o7 mustEqual None
        e2.o8 mustEqual None
        e2.o9 mustEqual None
        e2.o10.map(_.toList) mustEqual None
        e2.o11 mustEqual None
    }
}
