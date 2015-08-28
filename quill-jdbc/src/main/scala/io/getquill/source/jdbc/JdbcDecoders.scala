package io.getquill.source.jdbc

import java.sql.ResultSet
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import java.sql.Clob
import java.sql.Blob
import java.sql.Array
import java.sql.Struct
import java.sql.Ref
import java.sql
import java.util

trait JdbcDecoders {
  this: JdbcSource =>

  private def decoder[T](f: ResultSet => Int => T): Decoder[T] =
    new Decoder[T] {
      def apply(index: Int, row: ResultSet) =
        f(row)(index + 1)
    }

  implicit val stringDecoder = decoder(_.getString)
  implicit val bigDecimalDecoder =
    decoder {
      rs =>
        (index: Int) =>
          BigDecimal(rs.getBigDecimal(index + 1))
    }
  implicit val booleanDecoder = decoder(_.getBoolean)
  implicit val byteDecoder = decoder(_.getByte)
  implicit val shortDecoder = decoder(_.getShort)
  implicit val intDecoder = decoder(_.getInt)
  implicit val longDecoder = decoder(_.getLong)
  implicit val floatDecoder = decoder(_.getFloat)
  implicit val doubleDecoder = decoder(_.getDouble)
  implicit val byteArrayDecoder = decoder(_.getBytes)
  implicit val dateDecoder = decoder {
    rs =>
      (index: Int) =>
        new util.Date(rs.getTimestamp(index + 1).getTime)
  }

  // java.sql

  implicit val sqlDateDecoder = decoder[sql.Date](_.getDate)
  implicit val sqlTimeDecoder = decoder(_.getTime)
  implicit val sqlTimestampDecoder = decoder(_.getTimestamp)
  implicit val sqlClobDecoder = decoder(_.getClob)
  implicit val sqlBlobDecoder = decoder(_.getBlob)
  implicit val sqlArrayDecoder = decoder(_.getArray)
  implicit val sqlRefDecoder = decoder(_.getRef)
}
