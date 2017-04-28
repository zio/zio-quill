package io.getquill.context.async

import java.time.LocalDate
import java.util.Date

import io.getquill.PostgresAsyncContext
import io.getquill.context.sql.dsl.ArrayEncoding
import org.joda.time.{ LocalDate => JodaLocalDate, LocalDateTime => JodaLocalDateTime }

trait ArrayEncoders extends ArrayEncoding {
  self: PostgresAsyncContext[_] =>

  implicit def arrayStringEncoder[Col <: Seq[String]]: Encoder[Col] = rawEncoder[String, Col]
  implicit def arrayBigDecimalEncoder[Col <: Seq[BigDecimal]]: Encoder[Col] = rawEncoder[BigDecimal, Col]
  implicit def arrayBooleanEncoder[Col <: Seq[Boolean]]: Encoder[Col] = rawEncoder[Boolean, Col]
  implicit def arrayByteEncoder[Col <: Seq[Byte]]: Encoder[Col] = rawEncoder[Byte, Col]
  implicit def arrayShortEncoder[Col <: Seq[Short]]: Encoder[Col] = rawEncoder[Short, Col]
  implicit def arrayIntEncoder[Col <: Seq[Index]]: Encoder[Col] = rawEncoder[Index, Col]
  implicit def arrayLongEncoder[Col <: Seq[Long]]: Encoder[Col] = rawEncoder[Long, Col]
  implicit def arrayFloatEncoder[Col <: Seq[Float]]: Encoder[Col] = rawEncoder[Float, Col]
  implicit def arrayDoubleEncoder[Col <: Seq[Double]]: Encoder[Col] = rawEncoder[Double, Col]
  implicit def arrayDateEncoder[Col <: Seq[Date]]: Encoder[Col] = rawEncoder[Date, Col]
  implicit def arrayLocalDateTimeJodaEncoder[Col <: Seq[JodaLocalDateTime]]: Encoder[Col] = rawEncoder[JodaLocalDateTime, Col]
  implicit def arrayLocalDateJodaEncoder[Col <: Seq[JodaLocalDate]]: Encoder[Col] = rawEncoder[JodaLocalDate, Col]
  implicit def arrayLocalDateEncoder[Col <: Seq[LocalDate]]: Encoder[Col] = rawEncoder[LocalDate, Col]

  def arrayEncoder[T, Col <: Seq[T]](mapper: T => Any): Encoder[Col] =
    encoder[Col]((col: Col) => col.toIndexedSeq.map(mapper), SqlTypes.ARRAY)

  private def rawEncoder[T, Col <: Seq[T]]: Encoder[Col] = arrayEncoder[T, Col](identity)
}
