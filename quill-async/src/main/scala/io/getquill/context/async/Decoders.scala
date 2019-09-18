package io.getquill.context.async

import java.time._
import java.util.Date

import io.getquill.util.Messages.fail
import org.joda.time.{ DateTime => JodaDateTime, LocalDate => JodaLocalDate, LocalTime => JodaLocalTime, LocalDateTime => JodaLocalDateTime }

import scala.reflect.{ ClassTag, classTag }

trait Decoders {
  this: AsyncContext[_, _, _] =>

  type Decoder[T] = AsyncDecoder[T]

  type DecoderSqlType = SqlTypes.SqlTypes

  case class AsyncDecoder[T](sqlType: DecoderSqlType)(implicit decoder: BaseDecoder[T])
    extends BaseDecoder[T] {
    override def apply(index: Index, row: ResultRow) =
      decoder(index, row)
  }

  def decoder[T: ClassTag](
    f:       PartialFunction[Any, T] = PartialFunction.empty,
    sqlType: DecoderSqlType
  ): Decoder[T] =
    AsyncDecoder[T](sqlType)(new BaseDecoder[T] {
      def apply(index: Index, row: ResultRow) = {
        row(index) match {
          case value: T                      => value
          case value if f.isDefinedAt(value) => f(value)
          case value =>
            fail(
              s"Value '$value' at index $index can't be decoded to '${classTag[T].runtimeClass}'"
            )
        }
      }
    })

  implicit def mappedDecoder[I, O](implicit mapped: MappedEncoding[I, O], decoder: Decoder[I]): Decoder[O] =
    AsyncDecoder(decoder.sqlType)(new BaseDecoder[O] {
      def apply(index: Index, row: ResultRow): O =
        mapped.f(decoder.apply(index, row))
    })

  trait NumericDecoder[T] extends BaseDecoder[T] {
    def apply(index: Index, row: ResultRow) =
      row(index) match {
        case v: Byte       => decode(v)
        case v: Short      => decode(v)
        case v: Int        => decode(v)
        case v: Long       => decode(v)
        case v: Float      => decode(v)
        case v: Double     => decode(v)
        case v: BigDecimal => decode(v)
        case other =>
          fail(s"Value $other is not numeric")
      }

    def decode[U](v: U)(implicit n: Numeric[U]): T
  }

  implicit def optionDecoder[T](implicit d: Decoder[T]): Decoder[Option[T]] =
    AsyncDecoder(d.sqlType)(new BaseDecoder[Option[T]] {
      def apply(index: Index, row: ResultRow) = {
        row(index) match {
          case null  => None
          case value => Some(d(index, row))
        }
      }
    })

  implicit val stringDecoder: Decoder[String] = decoder[String](PartialFunction.empty, SqlTypes.VARCHAR)

  implicit val bigDecimalDecoder: Decoder[BigDecimal] =
    AsyncDecoder(SqlTypes.REAL)(new NumericDecoder[BigDecimal] {
      def decode[U](v: U)(implicit n: Numeric[U]) =
        BigDecimal(n.toDouble(v))
    })

  implicit val booleanDecoder: Decoder[Boolean] =
    decoder[Boolean]({
      case v: Byte  => v == (1: Byte)
      case v: Short => v == (1: Short)
      case v: Int   => v == 1
      case v: Long  => v == 1L
    }, SqlTypes.BOOLEAN)

  implicit val byteDecoder: Decoder[Byte] =
    decoder[Byte]({
      case v: Short => v.toByte
    }, SqlTypes.TINYINT)

  implicit val shortDecoder: Decoder[Short] =
    decoder[Short]({
      case v: Byte => v.toShort
    }, SqlTypes.SMALLINT)

  implicit val intDecoder: Decoder[Int] =
    AsyncDecoder(SqlTypes.INTEGER)(new NumericDecoder[Int] {
      def decode[U](v: U)(implicit n: Numeric[U]) =
        n.toInt(v)
    })

  implicit val longDecoder: Decoder[Long] =
    AsyncDecoder(SqlTypes.BIGINT)(new NumericDecoder[Long] {
      def decode[U](v: U)(implicit n: Numeric[U]) =
        n.toLong(v)
    })

  implicit val floatDecoder: Decoder[Float] =
    AsyncDecoder(SqlTypes.FLOAT)(new NumericDecoder[Float] {
      def decode[U](v: U)(implicit n: Numeric[U]) =
        n.toFloat(v)
    })

  implicit val doubleDecoder: Decoder[Double] =
    AsyncDecoder(SqlTypes.DOUBLE)(new NumericDecoder[Double] {
      def decode[U](v: U)(implicit n: Numeric[U]) =
        n.toDouble(v)
    })

  implicit val byteArrayDecoder: Decoder[Array[Byte]] = decoder[Array[Byte]](PartialFunction.empty, SqlTypes.TINYINT)

  implicit val jodaDateTimeDecoder: Decoder[JodaDateTime] = decoder[JodaDateTime]({
    case dateTime: JodaDateTime           => dateTime
    case localDateTime: JodaLocalDateTime => localDateTime.toDateTime
  }, SqlTypes.TIMESTAMP)

  implicit val jodaLocalDateDecoder: Decoder[JodaLocalDate] = decoder[JodaLocalDate]({
    case localDate: JodaLocalDate => localDate
  }, SqlTypes.DATE)

  implicit val jodaLocalDateTimeDecoder: Decoder[JodaLocalDateTime] = decoder[JodaLocalDateTime]({
    case localDateTime: JodaLocalDateTime => localDateTime
  }, SqlTypes.TIMESTAMP)

  implicit val dateDecoder: Decoder[Date] = decoder[Date]({
    case localDateTime: JodaLocalDateTime => localDateTime.toDate
    case localDate: JodaLocalDate         => localDate.toDate
  }, SqlTypes.TIMESTAMP)

  implicit val decodeZonedDateTime: MappedEncoding[JodaDateTime, ZonedDateTime] =
    MappedEncoding(jdt => ZonedDateTime.ofInstant(Instant.ofEpochMilli(jdt.getMillis), ZoneId.of(jdt.getZone.getID)))

  implicit val decodeOffsetDateTime: MappedEncoding[JodaDateTime, OffsetDateTime] =
    MappedEncoding(jdt => OffsetDateTime.ofInstant(Instant.ofEpochMilli(jdt.getMillis), ZoneId.of(jdt.getZone.getID)))

  implicit val decodeLocalDate: MappedEncoding[JodaLocalDate, LocalDate] =
    MappedEncoding(jld => LocalDate.of(jld.getYear, jld.getMonthOfYear, jld.getDayOfMonth))

  implicit val decodeLocalTime: MappedEncoding[JodaLocalTime, LocalTime] =
    MappedEncoding(jlt => LocalTime.of(jlt.getHourOfDay, jlt.getMinuteOfHour, jlt.getSecondOfMinute))

  implicit val decodeLocalDateTime: MappedEncoding[JodaLocalDateTime, LocalDateTime] =
    MappedEncoding(jldt => LocalDateTime.ofInstant(jldt.toDate.toInstant, ZoneId.systemDefault()))

  implicit val localDateDecoder: Decoder[LocalDate] = mappedDecoder(decodeLocalDate, jodaLocalDateDecoder)
}
