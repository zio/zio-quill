package io.getquill.context.ndbc

import java.time._
import java.util.{ Date, UUID }

import scala.language.implicitConversions
import scala.math.BigDecimal.javaBigDecimal2bigDecimal

import io.getquill.context.sql.encoding.ArrayEncoding
import io.trane.ndbc.PostgresRow
import io.trane.ndbc.value.Value

class Default[+T](val default: T)

object Default {
  implicit def defaultNull[T <: AnyRef]: Default[T] = new Default[T](null.asInstanceOf[T])
  implicit def defaultNumeric[T <: Numeric[_]](n: T) = new Default[T](0.asInstanceOf[T])
  implicit object DefaultBoolean extends Default[Boolean](false)

  def value[A](implicit value: Default[A]): A = value.default
}

trait PostgresDecoders {
  this: NdbcContext[_, _, _, PostgresRow] with ArrayEncoding =>

  type Decoder[T] = BaseDecoder[T]

  protected val zoneOffset: ZoneOffset

  def decoder[T, U](f: PostgresRow => Int => T)(implicit map: T => U): Decoder[U] =
    (index, row) =>
      row.column(index) match {
        case Value.NULL => Default.value[U]
        case _          => map(f(row)(index))
      }

  def arrayDecoder[T, U, Col <: Seq[U]](f: PostgresRow => Int => Array[T])(implicit map: T => U, bf: CBF[U, Col]): Decoder[Col] =
    (index, row) => {
      f(row)(index).foldLeft(bf()) {
        case (b, v) => b += map(v)
      }.result()
    }

  implicit def mappedDecoder[I, O](implicit mapped: MappedEncoding[I, O], d: Decoder[I]): Decoder[O] =
    mappedBaseDecoder(mapped, d)

  implicit def optionDecoder[T](implicit d: Decoder[T]): Decoder[Option[T]] =
    (idx, row) =>
      row.column(idx) match {
        case Value.NULL => None
        case value      => Option(d(idx, row))
      }

  private implicit def toDate(v: LocalDateTime): Date = Date.from(v.toInstant(zoneOffset))

  implicit val uuidDecoder: Decoder[UUID] = decoder(_.getUUID)
  implicit val stringDecoder: Decoder[String] = decoder(_.getString)
  implicit val bigDecimalDecoder: Decoder[BigDecimal] = decoder(_.getBigDecimal)
  implicit val booleanDecoder: Decoder[Boolean] = decoder(_.getBoolean)
  implicit val byteDecoder: Decoder[Byte] = decoder(_.getByte)
  implicit val shortDecoder: Decoder[Short] = decoder(_.getShort)
  implicit val intDecoder: Decoder[Int] = decoder(_.getInteger)
  implicit val longDecoder: Decoder[Long] = decoder(_.getLong)
  implicit val floatDecoder: Decoder[Float] = decoder(_.getFloat)
  implicit val doubleDecoder: Decoder[Double] = decoder(_.getDouble)
  implicit val byteArrayDecoder: Decoder[Array[Byte]] = decoder(_.getByteArray)
  implicit val dateDecoder: Decoder[Date] = decoder(_.getLocalDateTime)
  implicit val localDateDecoder: Decoder[LocalDate] = decoder(_.getLocalDate)
  implicit val localDateTimeDecoder: Decoder[LocalDateTime] = decoder(_.getLocalDateTime)
  implicit val offsetTimeDecoder: Decoder[OffsetTime] = decoder(_.getOffsetTime)

  implicit def arrayStringDecoder[Col <: Seq[String]](implicit bf: CBF[String, Col]): Decoder[Col] = arrayDecoder[String, String, Col](_.getStringArray)
  implicit def arrayBigDecimalDecoder[Col <: Seq[BigDecimal]](implicit bf: CBF[BigDecimal, Col]): Decoder[Col] = arrayDecoder[java.math.BigDecimal, BigDecimal, Col](_.getBigDecimalArray)
  implicit def arrayBooleanDecoder[Col <: Seq[Boolean]](implicit bf: CBF[Boolean, Col]): Decoder[Col] = arrayDecoder[java.lang.Boolean, Boolean, Col](_.getBooleanArray)
  implicit def arrayByteDecoder[Col <: Seq[Byte]](implicit bf: CBF[Byte, Col]): Decoder[Col] = arrayDecoder[Byte, Byte, Col](_.getByteArray)
  implicit def arrayShortDecoder[Col <: Seq[Short]](implicit bf: CBF[Short, Col]): Decoder[Col] = arrayDecoder[java.lang.Short, Short, Col](_.getShortArray)
  implicit def arrayIntDecoder[Col <: Seq[Int]](implicit bf: CBF[Int, Col]): Decoder[Col] = arrayDecoder[java.lang.Integer, Int, Col](_.getIntegerArray)
  implicit def arrayLongDecoder[Col <: Seq[Long]](implicit bf: CBF[Long, Col]): Decoder[Col] = arrayDecoder[java.lang.Long, Long, Col](_.getLongArray)
  implicit def arrayFloatDecoder[Col <: Seq[Float]](implicit bf: CBF[Float, Col]): Decoder[Col] = arrayDecoder[java.lang.Float, Float, Col](_.getFloatArray)
  implicit def arrayDoubleDecoder[Col <: Seq[Double]](implicit bf: CBF[Double, Col]): Decoder[Col] = arrayDecoder[java.lang.Double, Double, Col](_.getDoubleArray)
  implicit def arrayDateDecoder[Col <: Seq[Date]](implicit bf: CBF[Date, Col]): Decoder[Col] = arrayDecoder[LocalDateTime, Date, Col](_.getLocalDateTimeArray)
  implicit def arrayLocalDateDecoder[Col <: Seq[LocalDate]](implicit bf: CBF[LocalDate, Col]): Decoder[Col] = arrayDecoder[LocalDate, LocalDate, Col](_.getLocalDateArray)
}
