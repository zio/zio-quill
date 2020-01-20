package io.getquill.context.async

import java.time.{ LocalDate, LocalDateTime, LocalTime, OffsetDateTime, ZoneId, ZonedDateTime }
import java.util.Date

import org.joda.time.{ DateTime => JodaDateTime, DateTimeZone => JodaDateTimeZone, LocalTime => JodaLocalTime, LocalDate => JodaLocalDate, LocalDateTime => JodaLocalDateTime }

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
  implicit val jodaDateTimeEncoder: Encoder[JodaDateTime] = encoder[JodaDateTime](SqlTypes.TIMESTAMP)
  implicit val jodaLocalDateEncoder: Encoder[JodaLocalDate] = encoder[JodaLocalDate](SqlTypes.DATE)
  implicit val jodaLocalDateTimeEncoder: Encoder[JodaLocalDateTime] = encoder[JodaLocalDateTime](SqlTypes.TIMESTAMP)
  implicit val dateEncoder: Encoder[Date] = encoder[Date]((d: Date) => new JodaLocalDateTime(d), SqlTypes.TIMESTAMP)

  implicit val encodeZonedDateTime: MappedEncoding[ZonedDateTime, JodaDateTime] =
    MappedEncoding(zdt => new JodaDateTime(zdt.toInstant.toEpochMilli, JodaDateTimeZone.forID(zdt.getZone.getId)))

  implicit val encodeOffsetDateTime: MappedEncoding[OffsetDateTime, JodaDateTime] =
    MappedEncoding(odt => new JodaDateTime(odt.toInstant.toEpochMilli, JodaDateTimeZone.forID(odt.getOffset.getId)))

  implicit val encodeLocalDate: MappedEncoding[LocalDate, JodaLocalDate] =
    MappedEncoding(ld => new JodaLocalDate(ld.getYear, ld.getMonthValue, ld.getDayOfMonth))

  implicit val encodeLocalTime: MappedEncoding[LocalTime, JodaLocalTime] =
    MappedEncoding(lt => new JodaLocalTime(lt.getHour, lt.getMinute, lt.getSecond))

  implicit val encodeLocalDateTime: MappedEncoding[LocalDateTime, JodaLocalDateTime] =
    MappedEncoding(ldt => new JodaLocalDateTime(ldt.atZone(ZoneId.systemDefault()).toInstant.toEpochMilli))

  implicit val localDateEncoder: Encoder[LocalDate] = mappedEncoder(encodeLocalDate, jodaLocalDateEncoder)
}
