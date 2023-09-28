package io.getquill.context.sql

import io.getquill.base.Spec

import java.time.{
  LocalDate,
  LocalDateTime,
  OffsetDateTime,
  OffsetTime,
  ZoneId,
  ZoneOffset,
  ZonedDateTime
}
import java.util.{Date, UUID}
import io.getquill.{ Delete, Quoted }
import org.scalatest.Assertion

final case class EncodingTestType(value: String)

final case class Number(value: String) extends AnyVal

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

  case class TimeEntity(
    sqlDate: java.sql.Date,                      // DATE
    sqlTime: java.sql.Time,                      // TIME
    sqlTimestamp: java.sql.Timestamp,            // DATETIME
    timeLocalDate: java.time.LocalDate,          // DATE
    timeLocalTime: java.time.LocalTime,          // TIME
    timeLocalDateTime: java.time.LocalDateTime,  // DATETIME
    timeZonedDateTime: java.time.ZonedDateTime,  // DATETIMEOFFSET
    timeInstant: java.time.Instant,              // DATETIMEOFFSET
    timeOffsetTime: java.time.OffsetTime,        // TIME
    timeOffsetDateTime: java.time.OffsetDateTime // DATETIMEOFFSET
  ) {
    override def equals(other: Any): Boolean =
      other match {
        case t: TimeEntity =>
          this.sqlDate == t.sqlDate &&
          this.sqlTime == t.sqlTime &&
          this.sqlTimestamp == t.sqlTimestamp &&
          this.timeLocalDate == t.timeLocalDate &&
          this.timeLocalTime == t.timeLocalTime &&
          this.timeLocalDateTime == t.timeLocalDateTime &&
          this.timeZonedDateTime.isEqual(t.timeZonedDateTime) &&
          this.timeInstant == t.timeInstant &&
          this.timeOffsetTime.isEqual(t.timeOffsetTime) &&
          this.timeOffsetDateTime.isEqual(t.timeOffsetDateTime)
        case _ => false
      }
  }

  object TimeEntity {
    case class TimeEntityInput(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int, nano: Int) {
      def toLocalDate: LocalDateTime = LocalDateTime.of(year, month, day, hour, minute, second, nano)
    }
    object TimeEntityInput {
      def default = new TimeEntityInput(2022, 1, 2, 3, 4, 6, 0)
    }
    def make(zoneIdRaw: ZoneId, timeEntity: TimeEntityInput = TimeEntityInput.default): TimeEntity = {
      val zoneId = zoneIdRaw.normalized()
      // Millisecond precisions in SQL Server and many contexts are wrong so not using them
      val nowInstant  = timeEntity.toLocalDate.atZone(zoneId).toInstant
      val nowDateTime = LocalDateTime.ofInstant(nowInstant, zoneId)
      val nowDate     = nowDateTime.toLocalDate
      val nowTime     = nowDateTime.toLocalTime
      val nowZoned    = ZonedDateTime.of(nowDateTime, zoneId)
      TimeEntity(
        java.sql.Date.valueOf(nowDate),
        java.sql.Time.valueOf(nowTime),
        java.sql.Timestamp.valueOf(nowDateTime),
        nowDate,
        nowTime,
        nowDateTime,
        nowZoned,
        nowInstant,
        OffsetTime.ofInstant(nowInstant, zoneId),
        OffsetDateTime.ofInstant(nowInstant, zoneId)
      )
    }
  }

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
    v11: Date,
    v12: EncodingTestType,
    v13: LocalDate,
    v14: UUID,
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
    o11: Option[Date],
    o12: Option[EncodingTestType],
    o13: Option[LocalDate],
    o14: Option[UUID],
    o15: Option[Number]
  )

  val delete: Quoted[Delete[EncodingTestEntity]] = quote {
    query[EncodingTestEntity].delete
  }

  val insert = quote { (e: EncodingTestEntity) =>
    query[EncodingTestEntity].insertValue(e)
  }

  val insertValues: Seq[EncodingTestEntity] =
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
        Date.from(LocalDateTime.of(2013, 11, 23, 0, 0, 0, 0).toInstant(ZoneOffset.UTC)),
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
        Some(Date.from(LocalDateTime.of(2013, 11, 23, 0, 0, 0, 0).toInstant(ZoneOffset.UTC))),
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
        0f,
        0d,
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

  def verify(result: List[EncodingTestEntity]): Unit = {
    result.size mustEqual insertValues.size
    result.zip(insertValues).foreach { case (e1, e2) =>
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

  val insertBarCode = quote((b: BarCode) => query[BarCode].insertValue(b).returningGenerated(_.uuid))
  val barCodeEntry: BarCode  = BarCode("returning UUID")

  def findBarCodeByUuid(uuid: UUID) = quote(query[BarCode].filter(_.uuid.forall(_ == lift(uuid))))

  def verifyBarcode(barCode: BarCode): Assertion = barCode.description mustEqual "returning UUID"
}
