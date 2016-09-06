package io.getquill.context.sql

import java.util.{ Date, UUID }

import scala.BigDecimal
import io.getquill.Spec

case class EncodingTestType(value: String)

trait EncodingSpec extends Spec {

  val context: SqlContext[_, _] with TestEncoders with TestDecoders

  import context._

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
    v12: EncodingTestType,
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
    o11: Option[Date],
    o12: Option[EncodingTestType]
  )

  val delete = quote {
    query[EncodingTestEntity].delete
  }

  val insert = quote {
    (e: EncodingTestEntity) => query[EncodingTestEntity].insert(e)
  }

  val insertValues =
    List(
      EncodingTestEntity(
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
        EncodingTestType("s"),
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
        Some(new Date(31200000)),
        Some(EncodingTestType("s"))
      ),
      EncodingTestEntity(
        "",
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
        EncodingTestType(""),
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
        e1.v12 mustEqual EncodingTestType("s")

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
        e1.o12 mustEqual Some(EncodingTestType("s"))

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
        e2.v12 mustEqual EncodingTestType("")

        e2.o1 mustEqual None
        e2.o2 mustEqual None
        e2.o3 mustEqual None
        e2.o4 mustEqual None
        e2.o5 mustEqual None
        e2.o6 mustEqual None
        e2.o7 mustEqual None
        e2.o8 mustEqual None
        e2.o9 mustEqual None
        e2.o10 mustEqual None
        e2.o11 mustEqual None
        e2.o12 mustEqual None
    }

  case class BarCode(description: String, uuid: Option[UUID] = None)

  def insertBarCode(implicit e: Encoder[UUID]) = quote((b: BarCode) => query[BarCode].insert(b).returning(_.uuid))
  val barCodeEntry = BarCode("returning UUID")

  def findBarCodeByUuid(uuid: UUID)(implicit enc: Encoder[UUID]) = quote(query[BarCode].filter(_.uuid == lift(Option(uuid))))

  def verifyBarcode(barCode: BarCode) = barCode.description mustEqual "returning UUID"
}
