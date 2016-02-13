package io.getquill.sources.jdbc

import java.sql.ResultSet
import java.util
import java.util.Calendar

import scala.math.BigDecimal.javaBigDecimal2bigDecimal

trait JdbcDecoders {
  this: JdbcSource[_, _] =>

  private def decoder[T](f: ResultSet => Int => T): Decoder[T] =
    new Decoder[T] {
      def apply(index: Int, row: ResultSet) =
        f(row)(index + 1)
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

  implicit val stringDecoder = decoder(_.getString)
  implicit val bigDecimalDecoder: Decoder[BigDecimal] =
    new Decoder[BigDecimal] {
      def apply(index: Int, row: ResultSet) = {
        val v = row.getBigDecimal(index + 1)
        if (v == null)
          BigDecimal(0)
        else
          v
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
        val v = row.getTimestamp(index + 1, Calendar.getInstance(dateTimeZone))
        if (v == null)
          new util.Date(0)
        else
          new util.Date(v.getTime)
      }
    }
}
