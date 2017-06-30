package io.getquill.context.orientdb

import java.util.Date

import io.getquill.NamingStrategy
import io.getquill.context.Context
import io.getquill.context.orientdb.dsl.OrientDBDsl

trait OrientDBContext[Naming <: NamingStrategy]
  extends Context[OrientDBIdiom, Naming]
  with OrientDBDsl {

  implicit def optionDecoder[T](implicit d: Decoder[T]): Decoder[Option[T]]
  implicit def optionEncoder[T](implicit d: Encoder[T]): Encoder[Option[T]]

  implicit val stringDecoder: Decoder[String]
  implicit val doubleDecoder: Decoder[Double]
  implicit val bigDecimalDecoder: Decoder[BigDecimal]
  implicit val booleanDecoder: Decoder[Boolean]
  implicit val shortDecoder: Decoder[Short]
  implicit val intDecoder: Decoder[Int]
  implicit val longDecoder: Decoder[Long]
  implicit val floatDecoder: Decoder[Float]
  implicit val byteArrayDecoder: Decoder[Array[Byte]]
  implicit val dateDecoder: Decoder[Date]

  implicit val stringEncoder: Encoder[String]
  implicit val bigDecimalEncoder: Encoder[BigDecimal]
  implicit val booleanEncoder: Encoder[Boolean]
  implicit val shortEncoder: Encoder[Short]
  implicit val intEncoder: Encoder[Int]
  implicit val longEncoder: Encoder[Long]
  implicit val floatEncoder: Encoder[Float]
  implicit val doubleEncoder: Encoder[Double]
  implicit val dateEncoder: Encoder[Date]
  implicit val byteArrayEncoder: Encoder[Array[Byte]]

  implicit def listDecoder[T]: Decoder[List[T]]
  implicit def setDecoder[T]: Decoder[Set[T]]
  implicit def mapDecoder[K, V]: Decoder[Map[K, V]]

  implicit def listEncoder[T]: Encoder[List[T]]
  implicit def setEncoder[T]: Encoder[Set[T]]
  implicit def mapEncoder[K, V]: Encoder[Map[K, V]]
}