package io.getquill.context.async

import java.time.LocalDate
import java.util.Date

import io.getquill.PostgresAsyncContext
import io.getquill.context.sql.dsl.ArrayEncoding
import io.getquill.util.Messages.fail
import org.joda.time.{ LocalDate => JodaLocalDate, LocalDateTime => JodaLocalDateTime }

import scala.reflect.ClassTag

/**
 * Base trait for encoding sql arrays via async driver.
 * We say `array` only in scope of sql driver. In Quill we represent them as instances of Traversable
 */
trait ArrayAsyncEncoding extends ArrayEncoding {
  self: PostgresAsyncContext[_] =>

  implicit def arrayStringEncoder[Col <: Traversable[String]]: Encoder[Col] = rawEncoder[String, Col]
  implicit def arrayBigDecimalEncoder[Col <: Traversable[BigDecimal]]: Encoder[Col] = rawEncoder[BigDecimal, Col]
  implicit def arrayBooleanEncoder[Col <: Traversable[Boolean]]: Encoder[Col] = rawEncoder[Boolean, Col]
  implicit def arrayByteEncoder[Col <: Traversable[Byte]]: Encoder[Col] = rawEncoder[Byte, Col]
  implicit def arrayShortEncoder[Col <: Traversable[Short]]: Encoder[Col] = rawEncoder[Short, Col]
  implicit def arrayIntEncoder[Col <: Traversable[Index]]: Encoder[Col] = rawEncoder[Index, Col]
  implicit def arrayLongEncoder[Col <: Traversable[Long]]: Encoder[Col] = rawEncoder[Long, Col]
  implicit def arrayFloatEncoder[Col <: Traversable[Float]]: Encoder[Col] = rawEncoder[Float, Col]
  implicit def arrayDoubleEncoder[Col <: Traversable[Double]]: Encoder[Col] = rawEncoder[Double, Col]
  implicit def arrayDateEncoder[Col <: Traversable[Date]]: Encoder[Col] = rawEncoder[Date, Col]
  implicit def arrayLocalDateTimeJodaEncoder[Col <: Traversable[JodaLocalDateTime]]: Encoder[Col] = rawEncoder[JodaLocalDateTime, Col]
  implicit def arrayLocalDateJodaEncoder[Col <: Traversable[JodaLocalDate]]: Encoder[Col] = rawEncoder[JodaLocalDate, Col]
  implicit def arrayLocalDateEncoder[Col <: Traversable[LocalDate]]: Encoder[Col] = rawEncoder[LocalDate, Col]

  implicit def arrayStringDecoder[Col <: Traversable[String]](implicit bf: CBF[String, Col]): Decoder[Col] = rawDecoder[String, Col]
  implicit def arrayBigDecimalDecoder[Col <: Traversable[BigDecimal]](implicit bf: CBF[BigDecimal, Col]): Decoder[Col] = rawDecoder[BigDecimal, Col]
  implicit def arrayBooleanDecoder[Col <: Traversable[Boolean]](implicit bf: CBF[Boolean, Col]): Decoder[Col] = rawDecoder[Boolean, Col]
  implicit def arrayByteDecoder[Col <: Traversable[Byte]](implicit bf: CBF[Byte, Col]): Decoder[Col] = arrayDecoder[Short, Byte, Col](_.toByte)
  implicit def arrayShortDecoder[Col <: Traversable[Short]](implicit bf: CBF[Short, Col]): Decoder[Col] = rawDecoder[Short, Col]
  implicit def arrayIntDecoder[Col <: Traversable[Index]](implicit bf: CBF[Index, Col]): Decoder[Col] = rawDecoder[Index, Col]
  implicit def arrayLongDecoder[Col <: Traversable[Long]](implicit bf: CBF[Long, Col]): Decoder[Col] = rawDecoder[Long, Col]
  implicit def arrayFloatDecoder[Col <: Traversable[Float]](implicit bf: CBF[Float, Col]): Decoder[Col] = arrayDecoder[Double, Float, Col](_.toFloat)
  implicit def arrayDoubleDecoder[Col <: Traversable[Double]](implicit bf: CBF[Double, Col]): Decoder[Col] = rawDecoder[Double, Col]
  implicit def arrayDateDecoder[Col <: Traversable[Date]](implicit bf: CBF[Date, Col]): Decoder[Col] = arrayDecoder[JodaLocalDateTime, Date, Col](_.toDate)
  implicit def arrayLocalDateTimeJodaDecoder[Col <: Traversable[JodaLocalDateTime]](implicit bf: CBF[JodaLocalDateTime, Col]): Decoder[Col] = rawDecoder[JodaLocalDateTime, Col]
  implicit def arrayLocalDateJodaDecoder[Col <: Traversable[JodaLocalDate]](implicit bf: CBF[JodaLocalDate, Col]): Decoder[Col] = rawDecoder[JodaLocalDate, Col]
  implicit def arrayLocalDateDecoder[Col <: Traversable[LocalDate]](implicit bf: CBF[LocalDate, Col]): Decoder[Col] =
    arrayDecoder[JodaLocalDate, LocalDate, Col](d => LocalDate.of(d.getYear, d.getMonthOfYear, d.getDayOfMonth))

  def arrayEncoder[T, Col <: Traversable[T]](mapper: T => Any): Encoder[Col] =
    encoder[Col]((col: Col) => col.toIndexedSeq.map(mapper), SqlTypes.ARRAY)

  def arrayDecoder[I, O, Col <: Traversable[O]](mapper: I => O)(implicit bf: CBF[O, Col], iTag: ClassTag[I], oTag: ClassTag[O]): Decoder[Col] =
    AsyncDecoder[Col](SqlTypes.ARRAY)(new BaseDecoder[Col] {
      def apply(index: Index, row: ResultRow): Col = {
        row(index) match {
          case seq: IndexedSeq[Any] => seq.foldLeft(bf()) {
            case (b, x: I) => b += mapper(x)
            case (_, x)    => fail(s"Array at index $index contains element of ${x.getClass.getCanonicalName}, but expected $iTag")
          }.result()
          case value => fail(
            s"Value '$value' at index $index is not an array so it cannot be decoded to collection of $oTag"
          )
        }
      }
    })

  private def rawEncoder[T, Col <: Traversable[T]]: Encoder[Col] = arrayEncoder[T, Col](identity)

  private def rawDecoder[T: ClassTag, Col <: Traversable[T]](implicit bf: CBF[T, Col]): Decoder[Col] =
    arrayDecoder[T, T, Col](identity)
}
