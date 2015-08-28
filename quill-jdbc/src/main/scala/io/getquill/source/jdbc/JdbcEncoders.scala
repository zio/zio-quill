package io.getquill.source.jdbc

import java.sql
import java.sql.PreparedStatement

trait JdbcEncoders {
  this: JdbcSource =>

  private def encoder[T](f: PreparedStatement => (Int, T) => Unit): Encoder[T] =
    new Encoder[T] {
      override def apply(index: Int, value: T, row: PreparedStatement) = {
        f(row)(index + 1, value)
        row
      }
    }

  implicit val stringEncoder = encoder(_.setString)
  implicit val bigDecimalEncoder = encoder(_.setBigDecimal)
  implicit val booleanEncoder = encoder(_.setBoolean)
  implicit val byteEncoder = encoder(_.setByte)
  implicit val shortEncoder = encoder(_.setShort)
  implicit val intEncoder = encoder(_.setInt)
  implicit val longEncoder = encoder(_.setLong)
  implicit val floatEncoder = encoder(_.setFloat)
  implicit val doubleEncoder = encoder(_.setDouble)
  implicit val byteArrayEncoder = encoder(_.setBytes)

  // java.sql

  implicit val sqlDateEncoder = encoder(_.setDate)
  implicit val sqlTimeEncoder = encoder(_.setTime)
  implicit val sqlTimestampEncoder = encoder(_.setTimestamp)
  implicit val sqlClobEncoder = encoder[sql.Clob](_.setClob)
  implicit val sqlBlobEncoder = encoder[sql.Blob](_.setBlob)
  implicit val sqlArrayEncoder = encoder(_.setArray)
  implicit val sqlRefEncoder = encoder(_.setRef)
}
