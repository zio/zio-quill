package io.getquill.context.sql.encoding

import java.time.LocalDate
import java.util.Date

import io.getquill.context.sql.SqlContext

import scala.collection.compat._
import scala.language.higherKinds

trait ArrayEncoding {
  self: SqlContext[_, _] =>

  type CBF[T, Col] = Factory[T, Col]

  implicit def arrayStringEncoder[Col <: Seq[String]]: Encoder[Col]
  implicit def arrayBigDecimalEncoder[Col <: Seq[BigDecimal]]: Encoder[Col]
  implicit def arrayBooleanEncoder[Col <: Seq[Boolean]]: Encoder[Col]
  implicit def arrayByteEncoder[Col <: Seq[Byte]]: Encoder[Col]
  implicit def arrayShortEncoder[Col <: Seq[Short]]: Encoder[Col]
  implicit def arrayIntEncoder[Col <: Seq[Int]]: Encoder[Col]
  implicit def arrayLongEncoder[Col <: Seq[Long]]: Encoder[Col]
  implicit def arrayFloatEncoder[Col <: Seq[Float]]: Encoder[Col]
  implicit def arrayDoubleEncoder[Col <: Seq[Double]]: Encoder[Col]
  implicit def arrayDateEncoder[Col <: Seq[Date]]: Encoder[Col]
  implicit def arrayLocalDateEncoder[Col <: Seq[LocalDate]]: Encoder[Col]

  implicit def arrayStringDecoder[Col <: Seq[String]](implicit bf: CBF[String, Col]): Decoder[Col]
  implicit def arrayBigDecimalDecoder[Col <: Seq[BigDecimal]](implicit bf: CBF[BigDecimal, Col]): Decoder[Col]
  implicit def arrayBooleanDecoder[Col <: Seq[Boolean]](implicit bf: CBF[Boolean, Col]): Decoder[Col]
  implicit def arrayByteDecoder[Col <: Seq[Byte]](implicit bf: CBF[Byte, Col]): Decoder[Col]
  implicit def arrayShortDecoder[Col <: Seq[Short]](implicit bf: CBF[Short, Col]): Decoder[Col]
  implicit def arrayIntDecoder[Col <: Seq[Int]](implicit bf: CBF[Int, Col]): Decoder[Col]
  implicit def arrayLongDecoder[Col <: Seq[Long]](implicit bf: CBF[Long, Col]): Decoder[Col]
  implicit def arrayFloatDecoder[Col <: Seq[Float]](implicit bf: CBF[Float, Col]): Decoder[Col]
  implicit def arrayDoubleDecoder[Col <: Seq[Double]](implicit bf: CBF[Double, Col]): Decoder[Col]
  implicit def arrayDateDecoder[Col <: Seq[Date]](implicit bf: CBF[Date, Col]): Decoder[Col]
  implicit def arrayLocalDateDecoder[Col <: Seq[LocalDate]](implicit bf: CBF[LocalDate, Col]): Decoder[Col]

  implicit def arrayMappedEncoder[I, O, Col[X] <: Seq[X]](
    implicit
    mapped: MappedEncoding[I, O],
    e:      Encoder[Seq[O]]
  ): Encoder[Col[I]] = {
    mappedEncoder[Col[I], Seq[O]](MappedEncoding((col: Col[I]) => col.map(mapped.f)), e)
  }

  implicit def arrayMappedDecoder[I, O, Col[X] <: Seq[X]](
    implicit
    mapped: MappedEncoding[I, O],
    d:      Decoder[Seq[I]],
    bf:     Factory[O, Col[O]]
  ): Decoder[Col[O]] = {
    mappedDecoder[Seq[I], Col[O]](MappedEncoding((col: Seq[I]) =>
      col.foldLeft(bf.newBuilder)((b, x) => b += mapped.f(x)).result), d)
  }
}
