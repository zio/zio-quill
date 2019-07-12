package io.getquill.context.sql

import java.time.LocalDate
import java.util.{ Date, UUID }

import io.getquill.Spec

case class EncodingTestType(value: String)

case class Number(value: String) extends AnyVal

object Number {
  def withValidation(value: String): Option[Number] =
    if (value.forall(_.isDigit))
      Some(Number(value))
    else
      None
}

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
    v13: LocalDate,
    v14: UUID,
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
    o12: Option[EncodingTestType],
    o13: Option[LocalDate],
    o14: Option[UUID],
    o15: Option[Number]
  )

  val delete = quote {
    query[EncodingTestEntity].delete
  }

  val insert = quote {
    (e: EncodingTestEntity) => query[EncodingTestEntity].insert(e)
  }

  val insertValues =
    Seq(
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
        LocalDate.of(2013, 11, 23),
        UUID.randomUUID(),
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
        Some(EncodingTestType("s")),
        Some(LocalDate.of(2013, 11, 23)),
        Some(UUID.randomUUID()),
        Some(Number("0"))
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
        LocalDate.ofEpochDay(0),
        UUID.randomUUID(),
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
        None,
        None,
        None,
        None
      )
    )

  def verify(result: List[EncodingTestEntity]) = {
    result.size mustEqual insertValues.size
    result.zip(insertValues).foreach {
      case (e1, e2) =>
        e1.v1 mustEqual e2.v1
        e1.v2 mustEqual e2.v2
        e1.v3 mustEqual e2.v3
        e1.v4 mustEqual e2.v4
        e1.v5 mustEqual e2.v5
        e1.v6 mustEqual e2.v6
        e1.v7 mustEqual e2.v7
        e1.v8 mustEqual e2.v8
        e1.v9 mustEqual e2.v9
        e1.v10 mustEqual e2.v10
        e1.v11 mustEqual e2.v11
        e1.v12 mustEqual e2.v12
        e1.v13 mustEqual e2.v13
        e1.v14 mustEqual e2.v14

        e1.o1 mustEqual e2.o1
        e1.o2 mustEqual e2.o2
        e1.o3 mustEqual e2.o3
        e1.o4 mustEqual e2.o4
        e1.o5 mustEqual e2.o5
        e1.o6 mustEqual e2.o6
        e1.o7 mustEqual e2.o7
        e1.o8 mustEqual e2.o8
        e1.o9 mustEqual e2.o9
        e1.o10.getOrElse(Array()) mustEqual e2.o10.getOrElse(Array())
        e1.o11 mustEqual e2.o11
        e1.o12 mustEqual e2.o12
        e1.o13 mustEqual e2.o13
        e1.o14 mustEqual e2.o14
        e1.o15 mustEqual e2.o15
    }
  }

  case class BarCode(description: String, uuid: Option[UUID] = None)

  val insertBarCode = quote((b: BarCode) => query[BarCode].insert(b).returningGenerated(_.uuid))
  val barCodeEntry = BarCode("returning UUID")

  def findBarCodeByUuid(uuid: UUID) = quote(query[BarCode].filter(_.uuid.forall(_ == lift(uuid))))

  def verifyBarcode(barCode: BarCode) = barCode.description mustEqual "returning UUID"
}
