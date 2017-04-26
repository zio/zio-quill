package io.getquill

import io.getquill.context.cassandra.{ CassandraContext, CqlIdiom, MappedType }

class CassandraMirrorContextWithQueryProbing extends CassandraMirrorContext with QueryProbing

class CassandraMirrorContext[Naming <: NamingStrategy]
  extends MirrorContext[CqlIdiom, Naming] with CassandraContext[Naming] {

  implicit def genericDecoder[T, Cas](implicit mapped: MappedType[T, Cas]): Decoder[T] = decoder[T](null)
  implicit def genericEncoder[T, Cas](implicit mapped: MappedType[T, Cas]): Encoder[T] = encoder[T]

  implicit def listDecoder[T, Cas](implicit mapped: MappedType[T, Cas]): Decoder[List[T]] = decoder[List[T]](null)
  implicit def listEncoder[T, Cas](implicit mapped: MappedType[T, Cas]): Encoder[List[T]] = encoder[List[T]]

  implicit def setDecoder[T, Cas](implicit mapped: MappedType[T, Cas]): Decoder[Set[T]] = decoder[Set[T]](null)
  implicit def setEncoder[T, Cas](implicit mapped: MappedType[T, Cas]): Encoder[Set[T]] = encoder[Set[T]]

  implicit def mapDecoder[K, V, KCas, VCas](implicit km: MappedType[K, KCas], vm: MappedType[V, VCas]): Decoder[Map[K, V]] =
    decoder[Map[K, V]](null)
  implicit def mapEncoder[K, V, KCas, VCas](implicit km: MappedType[K, KCas], vm: MappedType[V, VCas]): Encoder[Map[K, V]] =
    encoder[Map[K, V]]
}