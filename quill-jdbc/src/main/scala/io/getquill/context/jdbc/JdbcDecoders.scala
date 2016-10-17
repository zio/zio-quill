package io.getquill.context.jdbc

import java.sql.{ ResultSet, Types }
import java.time.{ LocalDate, LocalDateTime }
import java.util
import java.util.Calendar
import scala.math.BigDecimal.javaBigDecimal2bigDecimal
import io.getquill.JdbcContext

trait JdbcDecoders { this: JdbcContext[_, _] =>

  case class JdbcDecoder[T](sqlType: Int)(implicit decoder: Decoder[T])
    extends Decoder[T] {
    def apply(index: Int, row: ResultSet) = decoder.apply(index, row)
  }

  def decoder[T](f: ResultSet => Int => T, sqlType: Int): JdbcDecoder[T] =
    JdbcDecoder[T](sqlType) {
      new Decoder[T] {
        def apply(index: Int, row: ResultSet) =
          f(row)(index + 1)
      }
    }

  override protected def mappedDecoderImpl[I, O](implicit mapped: MappedEncoding[I, O], d: Decoder[I]): Decoder[O] =
    d match {
      case d @ JdbcDecoder(sqlType) =>
        val dec = new Decoder[O] {
          override def apply(index: Int, row: ResultRow): O = {
            mapped.f(d(index, row))
          }
        }
        JdbcDecoder(sqlType)(dec)
    }

  implicit def optionDecoder[T](implicit d: Decoder[T]): Decoder[Option[T]] =
    new Decoder[Option[T]] {
      def apply(index: Int, row: ResultSet) = {
        val res = d(index, row)
        row.wasNull match {
          case true  => None
          case false => Some(res)
        }
      }
    }

  implicit val stringDecoder = decoder(_.getString, Types.VARCHAR)
  implicit val bigDecimalDecoder: JdbcDecoder[BigDecimal] =
    JdbcDecoder[BigDecimal](Types.REAL)(new Decoder[BigDecimal] {
      def apply(index: Int, row: ResultSet) = {
        val v = row.getBigDecimal(index + 1)
        if (v == null)
          BigDecimal(0)
        else
          v
      }
    })
  implicit val booleanDecoder = decoder(_.getBoolean, Types.BOOLEAN)
  implicit val byteDecoder = decoder(_.getByte, Types.TINYINT)
  implicit val shortDecoder = decoder(_.getShort, Types.SMALLINT)
  implicit val intDecoder = decoder(_.getInt, Types.INTEGER)
  implicit val longDecoder = decoder(_.getLong, Types.BIGINT)
  implicit val floatDecoder = decoder(_.getFloat, Types.FLOAT)
  implicit val doubleDecoder = decoder(_.getDouble, Types.DOUBLE)
  implicit val byteArrayDecoder = decoder(_.getBytes, Types.ARRAY)
  implicit val dateDecoder: JdbcDecoder[util.Date] =
    JdbcDecoder[util.Date](Types.TIMESTAMP)(new Decoder[util.Date] {
      def apply(index: Int, row: ResultSet) = {
        val v = row.getTimestamp(index + 1, Calendar.getInstance(dateTimeZone))
        if (v == null)
          new util.Date(0)
        else
          new util.Date(v.getTime)
      }
    })
  implicit val localDateDecoder: JdbcDecoder[LocalDate] =
    JdbcDecoder(Types.DATE)(new Decoder[LocalDate] {
      def apply(index: Int, row: ResultSet) = {
        val v = row.getDate(index + 1, Calendar.getInstance(dateTimeZone))
        if (v == null)
          LocalDate.ofEpochDay(0)
        else
          v.toLocalDate
      }
    })
  implicit val localDateTimeDecoder: Decoder[LocalDateTime] =
    JdbcDecoder(Types.TIMESTAMP)(new Decoder[LocalDateTime] {
      def apply(index: Int, row: ResultSet) = {
        val v = row.getTimestamp(index + 1, Calendar.getInstance(dateTimeZone))
        if (v == null)
          LocalDate.ofEpochDay(0).atStartOfDay()
        else
          v.toLocalDateTime
      }
    })
}
