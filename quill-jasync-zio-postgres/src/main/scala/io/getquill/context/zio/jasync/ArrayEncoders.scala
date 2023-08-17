package io.getquill.context.zio.jasync

import java.sql.Timestamp
import java.time.{LocalDate, LocalDateTime, OffsetDateTime}
import java.util.Date
import io.getquill.context.sql.encoding.ArrayEncoding
import io.getquill.context.zio.{PostgresZioJAsyncContext, SqlTypes}

trait ArrayEncoders extends ArrayEncoding {
  self: PostgresZioJAsyncContext[_] =>

  implicit def arrayStringEncoder[Col <: Seq[String]]: Encoder[Col]         = arrayRawEncoder[String, Col]
  implicit def arrayBigDecimalEncoder[Col <: Seq[BigDecimal]]: Encoder[Col] = arrayRawEncoder[BigDecimal, Col]
  implicit def arrayBooleanEncoder[Col <: Seq[Boolean]]: Encoder[Col]       = arrayRawEncoder[Boolean, Col]
  implicit def arrayByteEncoder[Col <: Seq[Byte]]: Encoder[Col]             = arrayRawEncoder[Byte, Col]
  implicit def arrayShortEncoder[Col <: Seq[Short]]: Encoder[Col]           = arrayRawEncoder[Short, Col]
  implicit def arrayIntEncoder[Col <: Seq[Index]]: Encoder[Col]             = arrayRawEncoder[Index, Col]
  implicit def arrayLongEncoder[Col <: Seq[Long]]: Encoder[Col]             = arrayRawEncoder[Long, Col]
  implicit def arrayFloatEncoder[Col <: Seq[Float]]: Encoder[Col]           = arrayRawEncoder[Float, Col]
  implicit def arrayDoubleEncoder[Col <: Seq[Double]]: Encoder[Col]         = arrayRawEncoder[Double, Col]
  implicit def arrayDateEncoder[Col <: Seq[Date]]: Encoder[Col] =
    arrayEncoder[Date, Col](date => OffsetDateTime.ofInstant(date.toInstant, dateTimeZone).toLocalDateTime)
  implicit def arrayLocalDateEncoder[Col <: Seq[LocalDate]]: Encoder[Col]         = arrayRawEncoder[LocalDate, Col]
  implicit def arrayLocalDateTimeEncoder[Col <: Seq[LocalDateTime]]: Encoder[Col] = arrayRawEncoder[LocalDateTime, Col]

  def arrayEncoder[T, Col <: Seq[T]](mapper: T => Any): Encoder[Col] =
    encoder[Col]((col: Col) => col.toIndexedSeq.map(mapper).mkString("{", ",", "}"), SqlTypes.ARRAY)

  def arrayRawEncoder[T, Col <: Seq[T]]: Encoder[Col] = arrayEncoder[T, Col](identity)

}
