package io.getquill.context.jdbc

import java.sql.Types
import java.time.{ LocalDate, LocalDateTime }
import java.util
import java.util.Calendar

import scala.math.BigDecimal.javaBigDecimal2bigDecimal

trait Decoders {
  this: JdbcContext[_, _] =>

  type Decoder[T] = JdbcDecoder[T]

  case class JdbcDecoder[T](sqlType: Int, decoder: BaseDecoder[T]) extends BaseDecoder[T] {
    def apply(index: Index, row: ResultRow) =
      decoder(index + 1, row)
  }

  def decoder[T](sqlType: Int, d: BaseDecoder[T]): Decoder[T] =
    JdbcDecoder(sqlType, d)

  def decoder[T](sqlType: Int, f: ResultRow => Index => T): Decoder[T] =
    decoder(sqlType, (index, row) => f(row)(index))

  implicit def mappedDecoder[I, O](implicit mapped: MappedEncoding[I, O], d: Decoder[I]): Decoder[O] =
    JdbcDecoder(d.sqlType, mappedBaseDecoder(mapped, d.decoder))

  implicit def optionDecoder[T](implicit d: Decoder[T]): Decoder[Option[T]] =
    JdbcDecoder(
      d.sqlType,
      (index, row) => {
        try {
          val res = d.decoder(index, row)
          if (row.wasNull()) {
            None
          } else {
            Some(res)
          }
        } catch {
          case _: NullPointerException if row.wasNull() => None
        }
      }
    )

  implicit val stringDecoder: Decoder[String] = decoder(Types.VARCHAR, _.getString)
  implicit val bigDecimalDecoder: Decoder[BigDecimal] =
    decoder(Types.REAL, (index, row) => {
      val v = row.getBigDecimal(index)
      if (v == null)
        BigDecimal(0)
      else
        v
    })
  implicit val booleanDecoder: Decoder[Boolean] = decoder(Types.BOOLEAN, _.getBoolean)
  implicit val byteDecoder: Decoder[Byte] = decoder(Types.TINYINT, _.getByte)
  implicit val shortDecoder: Decoder[Short] = decoder(Types.SMALLINT, _.getShort)
  implicit val intDecoder: Decoder[Int] = decoder(Types.INTEGER, _.getInt)
  implicit val longDecoder: Decoder[Long] = decoder(Types.BIGINT, _.getLong)
  implicit val floatDecoder: Decoder[Float] = decoder(Types.FLOAT, _.getFloat)
  implicit val doubleDecoder: Decoder[Double] = decoder(Types.DOUBLE, _.getDouble)
  implicit val byteArrayDecoder: Decoder[Array[Byte]] = decoder(Types.ARRAY, _.getBytes)
  implicit val dateDecoder: Decoder[util.Date] =
    decoder(Types.TIMESTAMP, (index, row) => {
      val v = row.getTimestamp(index, Calendar.getInstance(dateTimeZone))
      if (v == null)
        new util.Date(0)
      else
        new util.Date(v.getTime)
    })
  implicit val localDateDecoder: Decoder[LocalDate] =
    decoder(Types.DATE, (index, row) => {
      val v = row.getDate(index, Calendar.getInstance(dateTimeZone))
      if (v == null)
        LocalDate.ofEpochDay(0)
      else
        v.toLocalDate
    })
  implicit val localDateTimeDecoder: Decoder[LocalDateTime] =
    decoder(Types.TIMESTAMP, (index, row) => {
      val v = row.getTimestamp(index, Calendar.getInstance(dateTimeZone))
      if (v == null)
        LocalDate.ofEpochDay(0).atStartOfDay()
      else
        v.toLocalDateTime
    })
}
