package io.getquill.context.sql.encoding

import java.time.LocalDate
import java.util.Date

import io.getquill.context.sql.SqlContext

import scala.collection.compat._
import scala.language.higherKinds

trait ArrayEncoding {
  self: SqlContext[_, _] =>

  type CBF[T, Col] = Factory[T, Col]

  implicit def arrayStringEncoder[Col <: collection.Seq[String]]: Encoder[Col]
  implicit def arrayBigDecimalEncoder[Col <: collection.Seq[BigDecimal]]: Encoder[Col]
  implicit def arrayBooleanEncoder[Col <: collection.Seq[Boolean]]: Encoder[Col]
  implicit def arrayByteEncoder[Col <: collection.Seq[Byte]]: Encoder[Col]
  implicit def arrayShortEncoder[Col <: collection.Seq[Short]]: Encoder[Col]
  implicit def arrayIntEncoder[Col <: collection.Seq[Int]]: Encoder[Col]
  implicit def arrayLongEncoder[Col <: collection.Seq[Long]]: Encoder[Col]
  implicit def arrayFloatEncoder[Col <: collection.Seq[Float]]: Encoder[Col]
  implicit def arrayDoubleEncoder[Col <: collection.Seq[Double]]: Encoder[Col]
  implicit def arrayDateEncoder[Col <: collection.Seq[Date]]: Encoder[Col]
  implicit def arrayLocalDateEncoder[Col <: collection.Seq[LocalDate]]: Encoder[Col]

  implicit def arrayStringDecoder[Col <: collection.Seq[String]](implicit bf: CBF[String, Col]): Decoder[Col]
  implicit def arrayBigDecimalDecoder[Col <: collection.Seq[BigDecimal]](implicit bf: CBF[BigDecimal, Col]): Decoder[Col]
  implicit def arrayBooleanDecoder[Col <: collection.Seq[Boolean]](implicit bf: CBF[Boolean, Col]): Decoder[Col]
  implicit def arrayByteDecoder[Col <: collection.Seq[Byte]](implicit bf: CBF[Byte, Col]): Decoder[Col]
  implicit def arrayShortDecoder[Col <: collection.Seq[Short]](implicit bf: CBF[Short, Col]): Decoder[Col]
  implicit def arrayIntDecoder[Col <: collection.Seq[Int]](implicit bf: CBF[Int, Col]): Decoder[Col]
  implicit def arrayLongDecoder[Col <: collection.Seq[Long]](implicit bf: CBF[Long, Col]): Decoder[Col]
  implicit def arrayFloatDecoder[Col <: collection.Seq[Float]](implicit bf: CBF[Float, Col]): Decoder[Col]
  implicit def arrayDoubleDecoder[Col <: collection.Seq[Double]](implicit bf: CBF[Double, Col]): Decoder[Col]
  implicit def arrayDateDecoder[Col <: collection.Seq[Date]](implicit bf: CBF[Date, Col]): Decoder[Col]
  implicit def arrayLocalDateDecoder[Col <: collection.Seq[LocalDate]](implicit bf: CBF[LocalDate, Col]): Decoder[Col]

  implicit def arrayMappedEncoder[I, O, Col[X] <: collection.Seq[X]](implicit
    mapped: MappedEncoding[I, O],
    e: Encoder[collection.Seq[O]]
  ): Encoder[Col[I]] =
    mappedEncoder[Col[I], collection.Seq[O]](MappedEncoding((col: Col[I]) => col.map(mapped.f)), e)

  implicit def arrayMappedDecoder[I, O, Col[X] <: collection.Seq[X]](implicit
    mapped: MappedEncoding[I, O],
    d: Decoder[collection.Seq[I]],
    bf: Factory[O, Col[O]]
  ): Decoder[Col[O]] =
    mappedDecoder[collection.Seq[I], Col[O]](
      MappedEncoding((col: collection.Seq[I]) => col.foldLeft(bf.newBuilder)((b, x) => b += mapped.f(x)).result()),
      d
    )
}
