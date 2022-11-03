package io.getquill.context.jdbc

import java.time.{ Instant, LocalDate, LocalDateTime, LocalTime, OffsetDateTime, OffsetTime, ZoneOffset, ZonedDateTime }
import java.util
import java.util.{ Calendar, TimeZone }
import scala.math.BigDecimal.javaBigDecimal2bigDecimal

trait Decoders {
  this: JdbcContextTypes[_, _] =>

  type Decoder[T] = JdbcDecoder[T]

  case class JdbcDecoder[T](decoder: BaseDecoder[T]) extends BaseDecoder[T] {
    def apply(index: Index, row: ResultRow, session: Session) =
      decoder(index + 1, row, session)
  }

  def decoder[T](d: (Index, ResultRow, Session) => T): Decoder[T] =
    JdbcDecoder(d)

  def decoder[T](f: ResultRow => Index => T): Decoder[T] =
    decoder((index, row, session) => f(row)(index))

  implicit def mappedDecoder[I, O](implicit mapped: MappedEncoding[I, O], d: Decoder[I]): Decoder[O] =
    JdbcDecoder(mappedBaseDecoder(mapped, d.decoder))

  implicit def optionDecoder[T](implicit d: Decoder[T]): Decoder[Option[T]] =
    JdbcDecoder(
      (index, row, session) => {
        try {
          // According to the JDBC spec, we first need to read the object before `row.wasNull` works
          row.getObject(index)
          if (row.wasNull()) {
            None
          } else {
            Some(d.decoder(index, row, session))
          }
        } catch {
          case _: NullPointerException if row.wasNull() => None
        }
      }
    )

  implicit val sqlDateDecoder: Decoder[java.sql.Date] = decoder(_.getDate)
  implicit val sqlTimeDecoder: Decoder[java.sql.Time] = decoder(_.getTime)
  implicit val sqlTimestampDecoder: Decoder[java.sql.Timestamp] = decoder(_.getTimestamp)

  implicit val stringDecoder: Decoder[String] = decoder(_.getString)
  implicit val bigDecimalDecoder: Decoder[BigDecimal] =
    decoder((index, row, session) =>
      row.getBigDecimal(index))
  implicit val byteDecoder: Decoder[Byte] = decoder(_.getByte)
  implicit val shortDecoder: Decoder[Short] = decoder(_.getShort)
  implicit val intDecoder: Decoder[Int] = decoder(_.getInt)
  implicit val longDecoder: Decoder[Long] = decoder(_.getLong)
  implicit val floatDecoder: Decoder[Float] = decoder(_.getFloat)
  implicit val doubleDecoder: Decoder[Double] = decoder(_.getDouble)
  implicit val byteArrayDecoder: Decoder[Array[Byte]] = decoder(_.getBytes)
  implicit val dateDecoder: Decoder[util.Date] =
    decoder((index, row, session) =>
      new util.Date(row.getTimestamp(index, Calendar.getInstance(dateTimeZone)).getTime))
}

trait BasicTimeDecoders { self: Decoders =>
  def dateTimeZone: TimeZone

  implicit val localDateDecoder: Decoder[LocalDate] =
    decoder((index, row, session) =>
      row.getDate(index).toLocalDate)
  implicit val localTimeDecoder: Decoder[LocalTime] =
    decoder((index, row, session) =>
      row.getTime(index).toLocalTime)
  implicit val localDateTimeDecoder: Decoder[LocalDateTime] =
    decoder((index, row, session) =>
      row.getTimestamp(index).toLocalDateTime)

  implicit val zonedDateTimeDecoder: Decoder[ZonedDateTime] =
    decoder((index, row, session) =>
      ZonedDateTime.ofInstant(row.getTimestamp(index).toInstant, dateTimeZone.toZoneId))
  implicit val instantDecoder: Decoder[Instant] =
    decoder((index, row, session) =>
      row.getTimestamp(index).toInstant)

  implicit val offsetTimeDecoder: Decoder[OffsetTime] =
    decoder((index, row, session) => {
      val utcLocalTime = row.getTime(index).toLocalTime
      utcLocalTime.atOffset(ZoneOffset.UTC)
    })
  implicit val offsetDateTimeDecoder: Decoder[OffsetDateTime] =
    decoder((index, row, session) =>
      OffsetDateTime.ofInstant(row.getTimestamp(index).toInstant, dateTimeZone.toZoneId))
}

trait ObjectGenericTimeDecoders { self: Decoders =>
  implicit val localDateDecoder: Decoder[LocalDate] =
    decoder((index, row, session) =>
      row.getObject(index, classOf[LocalDate]))
  implicit val localTimeDecoder: Decoder[LocalTime] =
    decoder((index, row, session) =>
      row.getObject(index, classOf[LocalTime]))
  implicit val localDateTimeDecoder: Decoder[LocalDateTime] =
    decoder((index, row, session) =>
      row.getObject(index, classOf[LocalDateTime]))

  implicit val zonedDateTimeDecoder: Decoder[ZonedDateTime] =
    decoder((index, row, session) =>
      row.getObject(index, classOf[OffsetDateTime]).toZonedDateTime)
  implicit val instantDecoder: Decoder[Instant] =
    decoder((index, row, session) =>
      row.getObject(index, classOf[OffsetDateTime]).toInstant)

  implicit val offsetTimeDecoder: Decoder[OffsetTime] =
    decoder((index, row, session) =>
      row.getObject(index, classOf[OffsetTime]))
  implicit val offsetDateTimeDecoder: Decoder[OffsetDateTime] =
    decoder((index, row, session) =>
      row.getObject(index, classOf[OffsetDateTime]))
}