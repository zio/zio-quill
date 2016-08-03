package io.getquill.context.jdbc

import java.sql.ResultSet
import java.util
import java.util.Calendar

import scala.BigDecimal
import scala.math.BigDecimal.javaBigDecimal2bigDecimal
import io.getquill.JdbcContext

trait JdbcDecoders {
  this: JdbcContext[_, _] =>

  def decoder[T](f: ResultSet => Int => T): Decoder[T] =
    new Decoder[T] {
      def apply(index: Int, row: ResultSet) =
        f(row)(index + 1)
    }

  implicit def optionDecoder[T](implicit d: Decoder[T]): Decoder[Option[T]] =
    new Decoder[Option[T]] {
      def apply(index: Int, row: ResultSet) = {
        // don't invoke the parent decoder before the value was null checked
        row.wasNull match {
          case true  => None
          case false => Some(d(index, row))
        }
      }
    }

  implicit val stringDecoder = decoder(_.getString)
  implicit val bigDecimalDecoder: Decoder[BigDecimal] =
    new Decoder[BigDecimal] {
      def apply(index: Int, row: ResultSet) = {
        row.getBigDecimal(index + 1)
      }
    }
  implicit val booleanDecoder = decoder(_.getBoolean)
  implicit val byteDecoder = decoder(_.getByte)
  implicit val shortDecoder = decoder(_.getShort)
  implicit val intDecoder = decoder(_.getInt)
  implicit val longDecoder = decoder(_.getLong)
  implicit val floatDecoder = decoder(_.getFloat)
  implicit val doubleDecoder = decoder(_.getDouble)
  implicit val byteArrayDecoder = decoder(_.getBytes)
  implicit val dateDecoder: Decoder[util.Date] =
    new Decoder[util.Date] {
      def apply(index: Int, row: ResultSet) = {
        new util.Date(row.getTimestamp(index + 1, Calendar.getInstance(dateTimeZone)).getTime)
      }
    }
}
