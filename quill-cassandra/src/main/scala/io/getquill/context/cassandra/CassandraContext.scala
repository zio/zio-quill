package io.getquill.context.cassandra

import java.util.{ Date, UUID }

import io.getquill.NamingStrategy
import io.getquill.context.Context

import scala.reflect.ClassTag

trait CassandraContext[N <: NamingStrategy]
  extends Context[CqlIdiom, N]
  with Ops {

  implicit def optionDecoder[T](implicit d: Decoder[T]): Decoder[Option[T]]
  implicit def optionEncoder[T](implicit d: Encoder[T]): Encoder[Option[T]]

  implicit val stringDecoder: Decoder[String]
  implicit val bigDecimalDecoder: Decoder[BigDecimal]
  implicit val booleanDecoder: Decoder[Boolean]
  implicit val intDecoder: Decoder[Int]
  implicit val longDecoder: Decoder[Long]
  implicit val floatDecoder: Decoder[Float]
  implicit val doubleDecoder: Decoder[Double]
  implicit val byteArrayDecoder: Decoder[Array[Byte]]
  implicit val uuidDecoder: Decoder[UUID]
  implicit val dateDecoder: Decoder[Date]

  implicit val stringEncoder: Encoder[String]
  implicit val bigDecimalEncoder: Encoder[BigDecimal]
  implicit val booleanEncoder: Encoder[Boolean]
  implicit val intEncoder: Encoder[Int]
  implicit val longEncoder: Encoder[Long]
  implicit val floatEncoder: Encoder[Float]
  implicit val doubleEncoder: Encoder[Double]
  implicit val byteArrayEncoder: Encoder[Array[Byte]]
  implicit val uuidEncoder: Encoder[UUID]
  implicit val dateEncoder: Encoder[Date]

  implicit def listDecoder[T: ClassTag, Cas: ClassTag](implicit mapped: MappedType[T, Cas]): Decoder[List[T]]
  implicit def listEncoder[T, Cas](implicit mapped: MappedType[T, Cas]): Encoder[List[T]]

  implicit def setDecoder[T: ClassTag, Cas: ClassTag](implicit mapped: MappedType[T, Cas]): Decoder[Set[T]]
  implicit def setEncoder[T, Cas](implicit mapped: MappedType[T, Cas]): Encoder[Set[T]]

  implicit def mapDecoder[K: ClassTag, V: ClassTag, KCas: ClassTag, VCas: ClassTag](
    implicit
    km: MappedType[K, KCas],
    vm: MappedType[V, VCas]
  ): Decoder[Map[K, V]]
  implicit def mapEncoder[K, V, KCas, VCas](implicit km: MappedType[K, KCas], vm: MappedType[V, VCas]): Encoder[Map[K, V]]

}