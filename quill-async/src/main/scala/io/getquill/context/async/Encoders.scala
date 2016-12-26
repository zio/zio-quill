package io.getquill.context.async

import java.time.{ LocalDate, LocalDateTime, ZoneId }
import java.util.Date

import org.joda.time.{ LocalDate => JodaLocalDate, LocalDateTime => JodaLocalDateTime }

trait Encoders {
  this: AsyncContext[_, _, _] =>

  type Encoder[T] = AsyncEncoder[T]

  type EncoderSqlType = SqlTypes.SqlTypes

  case class AsyncEncoder[T](sqlType: DecoderSqlType)(implicit encoder: BaseEncoder[T])
    extends BaseEncoder[T] {
    override def apply(index: Index, value: T, row: PrepareRow) =
      encoder.apply(index, value, row)
  }

  def encoder[T](sqlType: DecoderSqlType): Encoder[T] =
    encoder(identity[T], sqlType)

  def encoder[T](f: T => Any, sqlType: DecoderSqlType): Encoder[T] =
    AsyncEncoder[T](sqlType)(new BaseEncoder[T] {
      def apply(index: Index, value: T, row: PrepareRow) =
        row :+ f(value)
    })

  implicit def mappedEncoder[I, O](implicit mapped: MappedEncoding[I, O], e: Encoder[O]): Encoder[I] =
    AsyncEncoder(e.sqlType)(new BaseEncoder[I] {
      def apply(index: Index, value: I, row: PrepareRow) =
        e(index, mapped.f(value), row)
    })

  implicit def optionEncoder[T](implicit d: Encoder[T]): Encoder[Option[T]] =
    AsyncEncoder(d.sqlType)(new BaseEncoder[Option[T]] {
      def apply(index: Index, value: Option[T], row: PrepareRow) = {
        value match {
          case None    => nullEncoder(index, null, row)
          case Some(v) => d(index, v, row)
        }
      }
    })

  private[this] val nullEncoder: Encoder[Null] = encoder[Null](SqlTypes.NULL)

  implicit val stringEncoder: Encoder[String] = encoder[String](SqlTypes.VARCHAR)
  implicit val bigDecimalEncoder: Encoder[BigDecimal] = encoder[BigDecimal](SqlTypes.REAL)
  implicit val booleanEncoder: Encoder[Boolean] = encoder[Boolean](SqlTypes.BOOLEAN)
  implicit val byteEncoder: Encoder[Byte] = encoder[Byte](SqlTypes.TINYINT)
  implicit val shortEncoder: Encoder[Short] = encoder[Short](SqlTypes.SMALLINT)
  implicit val intEncoder: Encoder[Int] = encoder[Int](SqlTypes.INTEGER)
  implicit val longEncoder: Encoder[Long] = encoder[Long](SqlTypes.BIGINT)
  implicit val floatEncoder: Encoder[Float] = encoder[Float](SqlTypes.FLOAT)
  implicit val doubleEncoder: Encoder[Double] = encoder[Double](SqlTypes.DOUBLE)
  implicit val byteArrayEncoder: Encoder[Array[Byte]] = encoder[Array[Byte]](SqlTypes.VARBINARY)
  implicit val dateEncoder: Encoder[Date] =
    encoder[Date]({ (value: Date) =>
      new JodaLocalDateTime(value)
    }, SqlTypes.TIMESTAMP)
  implicit val localDateEncoder: Encoder[LocalDate] =
    encoder[LocalDate]({ (value: LocalDate) =>
      new JodaLocalDate(
        value.getYear,
        value.getMonthValue,
        value.getDayOfMonth
      )
    }, SqlTypes.DATE)
  implicit val localDateTimeEncoder: Encoder[LocalDateTime] =
    encoder[LocalDateTime]({ (value: LocalDateTime) =>
      new JodaLocalDateTime(
        value.atZone(ZoneId.systemDefault()).toInstant.toEpochMilli
      )
    }, SqlTypes.TIMESTAMP)
}
