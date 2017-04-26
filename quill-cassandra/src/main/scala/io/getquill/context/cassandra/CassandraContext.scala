package io.getquill.context.cassandra

import io.getquill.NamingStrategy
import io.getquill.context.Context

trait CassandraContext[N <: NamingStrategy]
  extends Context[CqlIdiom, N]
  with Ops {

  implicit def genericDecoder[T, Cas](implicit mapped: MappedType[T, Cas]): Decoder[T]
  implicit def genericEncoder[T, Cas](implicit mapped: MappedType[T, Cas]): Encoder[T]

  implicit def optionDecoder[T](implicit d: Decoder[T]): Decoder[Option[T]]
  implicit def optionEncoder[T](implicit d: Encoder[T]): Encoder[Option[T]]

  implicit def listDecoder[T, Cas](implicit mapped: MappedType[T, Cas]): Decoder[List[T]]
  implicit def listEncoder[T, Cas](implicit mapped: MappedType[T, Cas]): Encoder[List[T]]

  implicit def setDecoder[T, Cas](implicit mapped: MappedType[T, Cas]): Decoder[Set[T]]
  implicit def setEncoder[T, Cas](implicit mapped: MappedType[T, Cas]): Encoder[Set[T]]

  implicit def mapDecoder[K, V, KCas, VCas](implicit km: MappedType[K, KCas], vm: MappedType[V, VCas]): Decoder[Map[K, V]]
  implicit def mapEncoder[K, V, KCas, VCas](implicit km: MappedType[K, KCas], vm: MappedType[V, VCas]): Encoder[Map[K, V]]
}