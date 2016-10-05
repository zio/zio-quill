package io.getquill.context.jdbc

import java.sql.{ Date, PreparedStatement, Timestamp, Types }
import java.time.{ LocalDate, LocalDateTime }
import java.util.{ Calendar, TimeZone }
import java.{ sql, util }

import io.getquill.JdbcContext
import io.getquill.util.Messages.fail

import scala.reflect.ClassTag

trait JdbcEncoders {
  this: JdbcContext[_, _] =>

  protected val dateTimeZone = TimeZone.getDefault

  case class JdbcEncoder[T](sqlType: Int)(implicit encoder: Encoder[T]) extends Encoder[T] {
    def apply(index: Int, value: T, row: PreparedStatement) =
      encoder.apply(index, value, row)
  }

  def encoder[T: ClassTag](f: PreparedStatement => (Int, T) => Unit, sqlType: Int): JdbcEncoder[T] =
    JdbcEncoder(sqlType)(new Encoder[T] {
      override def apply(index: Int, value: T, row: PreparedStatement) = {
        f(row)(index + 1, value)
        row
      }
    })

  override protected def mappedEncoderImpl[I, O](implicit mapped: MappedEncoding[I, O], e: Encoder[O]): Encoder[I] =
    e match {
      case e @ JdbcEncoder(sqlType) =>
        val enc = new Encoder[I] {
          override def apply(index: Int, value: I, row: PreparedStatement) =
            e(index, mapped.f(value), row)
        }
        JdbcEncoder(sqlType)(enc)
    }

  private[this] val nullEncoder = encoder[Int](_.setNull, Types.INTEGER)

  implicit def optionEncoder[T](implicit d: Encoder[T]): Encoder[Option[T]] =
    new Encoder[Option[T]] {
      override def apply(idx: Int, value: Option[T], row: PreparedStatement) =
        value match {
          case Some(v) => d(idx, v, row)
          case None => d match {
            case JdbcEncoder(sqlType) => nullEncoder(idx, sqlType, row)
            case _                    => fail("Can't determine SQL data type. JdbcEncoder is expected.")
          }
        }
    }

  implicit val stringEncoder: Encoder[String] = encoder(_.setString, Types.VARCHAR)
  implicit val bigDecimalEncoder: Encoder[BigDecimal] =
    encoder[BigDecimal](row => (idx, value) =>
      row.setBigDecimal(idx, value.bigDecimal), Types.NUMERIC)
  implicit val booleanEncoder: Encoder[Boolean] = encoder(_.setBoolean, Types.BOOLEAN)
  implicit val byteEncoder: Encoder[Byte] = encoder(_.setByte, Types.TINYINT)
  implicit val shortEncoder: Encoder[Short] = encoder(_.setShort, Types.SMALLINT)
  implicit val intEncoder: Encoder[Int] = encoder(_.setInt, Types.INTEGER)
  implicit val longEncoder: Encoder[Long] = encoder(_.setLong, Types.BIGINT)
  implicit val floatEncoder: Encoder[Float] = encoder(_.setFloat, Types.FLOAT)
  implicit val doubleEncoder: Encoder[Double] = encoder(_.setDouble, Types.DOUBLE)
  implicit val byteArrayEncoder: Encoder[Array[Byte]] = encoder(_.setBytes, Types.VARBINARY)
  implicit val dateEncoder: Encoder[util.Date] =
    encoder[util.Date](
      row => (idx, value) =>
        row.setTimestamp(idx, new sql.Timestamp(value.getTime), Calendar.getInstance(dateTimeZone)),
      Types.TIMESTAMP
    )
  implicit val localDateEncoder: Encoder[LocalDate] = encoder[LocalDate](
    row => (idx, value) =>
      row.setDate(idx, Date.valueOf(value), Calendar.getInstance(dateTimeZone)),
    Types.DATE
  )
  implicit val localDateTimeEncoder: Encoder[LocalDateTime] = encoder[LocalDateTime](
    row => (idx, value) =>
      row.setTimestamp(idx, Timestamp.valueOf(value), Calendar.getInstance(dateTimeZone)),
    Types.TIMESTAMP
  )
}