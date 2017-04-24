package io.getquill.context.sql.dsl

import java.time.LocalDate
import java.util.Date

import io.getquill.context.sql.SqlContext
import io.getquill.dsl.TraversableEncoding

import scala.collection.generic.CanBuildFrom

trait ArrayEncoding extends TraversableEncoding {
  self: SqlContext[_, _] =>

  type CBF[T, Col] = CanBuildFrom[Nothing, T, Col]

  implicit def arrayStringEncoder[Col <: Traversable[String]]: Encoder[Col]
  implicit def arrayBigDecimalEncoder[Col <: Traversable[BigDecimal]]: Encoder[Col]
  implicit def arrayBooleanEncoder[Col <: Traversable[Boolean]]: Encoder[Col]
  implicit def arrayByteEncoder[Col <: Traversable[Byte]]: Encoder[Col]
  implicit def arrayShortEncoder[Col <: Traversable[Short]]: Encoder[Col]
  implicit def arrayIntEncoder[Col <: Traversable[Int]]: Encoder[Col]
  implicit def arrayLongEncoder[Col <: Traversable[Long]]: Encoder[Col]
  implicit def arrayFloatEncoder[Col <: Traversable[Float]]: Encoder[Col]
  implicit def arrayDoubleEncoder[Col <: Traversable[Double]]: Encoder[Col]
  implicit def arrayDateEncoder[Col <: Traversable[Date]]: Encoder[Col]
  implicit def arrayLocalDateEncoder[Col <: Traversable[LocalDate]]: Encoder[Col]

  implicit def arrayStringDecoder[Col <: Traversable[String]](implicit bf: CBF[String, Col]): Decoder[Col]
  implicit def arrayBigDecimalDecoder[Col <: Traversable[BigDecimal]](implicit bf: CBF[BigDecimal, Col]): Decoder[Col]
  implicit def arrayBooleanDecoder[Col <: Traversable[Boolean]](implicit bf: CBF[Boolean, Col]): Decoder[Col]
  implicit def arrayByteDecoder[Col <: Traversable[Byte]](implicit bf: CBF[Byte, Col]): Decoder[Col]
  implicit def arrayShortDecoder[Col <: Traversable[Short]](implicit bf: CBF[Short, Col]): Decoder[Col]
  implicit def arrayIntDecoder[Col <: Traversable[Int]](implicit bf: CBF[Int, Col]): Decoder[Col]
  implicit def arrayLongDecoder[Col <: Traversable[Long]](implicit bf: CBF[Long, Col]): Decoder[Col]
  implicit def arrayFloatDecoder[Col <: Traversable[Float]](implicit bf: CBF[Float, Col]): Decoder[Col]
  implicit def arrayDoubleDecoder[Col <: Traversable[Double]](implicit bf: CBF[Double, Col]): Decoder[Col]
  implicit def arrayDateDecoder[Col <: Traversable[Date]](implicit bf: CBF[Date, Col]): Decoder[Col]
  implicit def arrayLocalDateDecoder[Col <: Traversable[LocalDate]](implicit bf: CBF[LocalDate, Col]): Decoder[Col]
}
