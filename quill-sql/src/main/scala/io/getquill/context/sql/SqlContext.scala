package io.getquill.context.sql

import java.time.LocalDate

import io.getquill.idiom.{ Idiom => BaseIdiom }
import java.util.{ Date, UUID }

import io.getquill.context.Context
import io.getquill.context.sql.dsl.SqlDsl
import io.getquill.NamingStrategy

trait SqlContext[Idiom <: BaseIdiom, Naming <: NamingStrategy]
  extends Context[Idiom, Naming]
  with SqlDsl {

  implicit val nullChecker: NullChecker

  implicit def optionDecoder[T](implicit d: Decoder[T]): Decoder[Option[T]]
  implicit def optionEncoder[T](implicit d: Encoder[T]): Encoder[Option[T]]

  implicit val stringDecoder: Decoder[String]
  implicit val bigDecimalDecoder: Decoder[BigDecimal]
  implicit val booleanDecoder: Decoder[Boolean]
  implicit val byteDecoder: Decoder[Byte]
  implicit val shortDecoder: Decoder[Short]
  implicit val intDecoder: Decoder[Int]
  implicit val longDecoder: Decoder[Long]
  implicit val floatDecoder: Decoder[Float]
  implicit val doubleDecoder: Decoder[Double]
  implicit val byteArrayDecoder: Decoder[Array[Byte]]
  implicit val dateDecoder: Decoder[Date]
  implicit val localDateDecoder: Decoder[LocalDate]
  implicit val uuidDecoder: Decoder[UUID]

  implicit val stringEncoder: Encoder[String]
  implicit val bigDecimalEncoder: Encoder[BigDecimal]
  implicit val booleanEncoder: Encoder[Boolean]
  implicit val byteEncoder: Encoder[Byte]
  implicit val shortEncoder: Encoder[Short]
  implicit val intEncoder: Encoder[Int]
  implicit val longEncoder: Encoder[Long]
  implicit val floatEncoder: Encoder[Float]
  implicit val doubleEncoder: Encoder[Double]
  implicit val byteArrayEncoder: Encoder[Array[Byte]]
  implicit val dateEncoder: Encoder[Date]
  implicit val localDateEncoder: Encoder[LocalDate]
  implicit val uuidEncoder: Encoder[UUID]
}
