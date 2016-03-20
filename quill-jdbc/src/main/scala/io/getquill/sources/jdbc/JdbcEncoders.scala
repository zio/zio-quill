package io.getquill.sources.jdbc

import java.sql
import java.util
import java.util.Calendar
import java.util.TimeZone
import java.sql.Types
import java.sql.PreparedStatement
import io.getquill.sources.BindedStatementBuilder

trait JdbcEncoders {
  this: JdbcSource[_, _] =>

  protected val dateTimeZone = TimeZone.getDefault

  def encoder[T](f: PreparedStatement => (Int, T) => Unit): Encoder[T] =
    new Encoder[T] {
      override def apply(index: Int, value: T, row: BindedStatementBuilder[PreparedStatement]) = {
        val raw = new io.getquill.sources.Encoder[PreparedStatement, T] {
          override def apply(index: Int, value: T, row: PreparedStatement) = {
            f(row)(index + 1, value)
            row
          }
        }
        row.single(index, value, raw)
      }
    }

  implicit def traversableEncoder[T](implicit enc: Encoder[T]): Encoder[Traversable[T]] =
    new Encoder[Traversable[T]] {
      override def apply(index: Int, values: Traversable[T], row: BindedStatementBuilder[PreparedStatement]) =
        row.coll(index, values, enc)
    }

  private[this] val nullEncoder = encoder[Int](_.setNull)

  implicit def optionEncoder[T](implicit d: Encoder[T]): Encoder[Option[T]] =
    new Encoder[Option[T]] {
      override def apply(idx: Int, value: Option[T], row: BindedStatementBuilder[PreparedStatement]) =
        value match {
          case Some(value) => d(idx, value, row)
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
            nullEncoder(idx, sqlType, row)
        }
    }

  implicit val stringEncoder: Encoder[String] = encoder(_.setString)
  implicit val bigDecimalEncoder: Encoder[BigDecimal] =
    encoder[BigDecimal] { row => (idx, value) =>
      row.setBigDecimal(idx, value.bigDecimal)
    }
  implicit val booleanEncoder: Encoder[Boolean] = encoder(_.setBoolean)
  implicit val byteEncoder: Encoder[Byte] = encoder(_.setByte)
  implicit val shortEncoder: Encoder[Short] = encoder(_.setShort)
  implicit val intEncoder: Encoder[Int] = encoder(_.setInt)
  implicit val longEncoder: Encoder[Long] = encoder(_.setLong)
  implicit val floatEncoder: Encoder[Float] = encoder(_.setFloat)
  implicit val doubleEncoder: Encoder[Double] = encoder(_.setDouble)
  implicit val byteArrayEncoder: Encoder[Array[Byte]] = encoder(_.setBytes)
  implicit val dateEncoder: Encoder[util.Date] =
    encoder[util.Date] { row => (idx, value) =>
      row.setTimestamp(idx, new sql.Timestamp(value.getTime), Calendar.getInstance(dateTimeZone))
    }
}
