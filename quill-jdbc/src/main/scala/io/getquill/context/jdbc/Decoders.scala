package io.getquill.context.jdbc

import java.time.{ Instant, LocalDate, LocalDateTime }
import java.util
import java.util.Calendar
import scala.math.BigDecimal.javaBigDecimal2bigDecimal

trait Decoders {
  this: JdbcComposition[_, _] =>

  type Decoder[T] = JdbcDecoder[T]

  case class JdbcDecoder[T](decoder: BaseDecoder[T]) extends BaseDecoder[T] {
    def apply(index: Index, row: ResultRow, session: Session) =
      decoder(index + 1, row, session)
  }

  def decoder[T](d: BaseDecoder[T]): Decoder[T] =
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
  implicit val localDateDecoder: Decoder[LocalDate] =
    decoder((index, row, session) =>
      row.getDate(index, Calendar.getInstance(dateTimeZone)).toLocalDate)
  implicit val localDateTimeDecoder: Decoder[LocalDateTime] =
    decoder((index, row, session) =>
      row.getTimestamp(index, Calendar.getInstance(dateTimeZone)).toLocalDateTime)
  implicit val instantDecoder: Decoder[Instant] =
    decoder((index, row, _) =>
      row.getTimestamp(index).toInstant)
}
