package io.getquill.sources.jdbc

import java.sql
import java.sql.PreparedStatement
import java.util
import java.util.Calendar
import java.util.TimeZone
import java.sql.Types

trait JdbcEncoders {
  this: JdbcSource[_, _] =>

  protected val dateTimeZone = TimeZone.getDefault

  private def encoder[T](f: PreparedStatement => (Int, T) => Unit): Encoder[T] =
    new Encoder[T] {
      override def apply(index: Int, value: T, row: PreparedStatement) = {
        f(row)(index + 1, value)
        row
      }
    }

  implicit def optionEncoder[T](implicit d: Encoder[T]): Encoder[Option[T]] =
    new Encoder[Option[T]] {
      override def apply(index: Int, value: Option[T], row: PreparedStatement) =
        value match {
          case Some(value) => d(index, value, row)
          case None =>
            import Types._
            val sqlType =
              d match {
                case `stringEncoder`     => VARCHAR
                case `bigDecimalEncoder` => NUMERIC
                case `booleanEncoder`    => BOOLEAN
                case `byteEncoder`       => TINYINT
                case `shortEncoder`      => SMALLINT
                case `intEncoder`        => INTEGER
                case `longEncoder`       => BIGINT
                case `floatEncoder`      => REAL
                case `doubleEncoder`     => DOUBLE
                case `byteArrayEncoder`  => VARBINARY
                case `dateEncoder`       => TIMESTAMP
              }
            row.setNull(index + 1, sqlType)
            row
        }
    }

  implicit val stringEncoder = encoder(_.setString)
  implicit val bigDecimalEncoder: Encoder[BigDecimal] =
    new Encoder[BigDecimal] {
      override def apply(index: Int, value: BigDecimal, row: PreparedStatement) = {
        row.setBigDecimal(index + 1, value.bigDecimal)
        row
      }
    }
  implicit val booleanEncoder = encoder(_.setBoolean)
  implicit val byteEncoder = encoder(_.setByte)
  implicit val shortEncoder = encoder(_.setShort)
  implicit val intEncoder = encoder(_.setInt)
  implicit val longEncoder = encoder(_.setLong)
  implicit val floatEncoder = encoder(_.setFloat)
  implicit val doubleEncoder = encoder(_.setDouble)
  implicit val byteArrayEncoder = encoder(_.setBytes)
  implicit val dateEncoder: Encoder[util.Date] =
    new Encoder[util.Date] {
      override def apply(index: Int, value: util.Date, row: PreparedStatement) = {
        row.setTimestamp(index + 1, new sql.Timestamp(value.getTime), Calendar.getInstance(dateTimeZone))
        row
      }
    }
}
