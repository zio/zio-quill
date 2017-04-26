package io.getquill

import io.getquill.context.cassandra.{ CassandraContext, CqlIdiom, MappedType }

import scala.reflect.ClassTag

class CassandraMirrorContextWithQueryProbing extends CassandraMirrorContext with QueryProbing

class CassandraMirrorContext[Naming <: NamingStrategy]
  extends MirrorContext[CqlIdiom, Naming] with CassandraContext[Naming] {

  implicit def listDecoder[T: ClassTag, Cas: ClassTag](implicit mapped: MappedType[T, Cas]): Decoder[List[T]] = decoder[List[T]]
  implicit def listEncoder[T, Cas](implicit mapped: MappedType[T, Cas]): Encoder[List[T]] = encoder[List[T]]

  implicit def setDecoder[T: ClassTag, Cas: ClassTag](implicit mapped: MappedType[T, Cas]): Decoder[Set[T]] = decoder[Set[T]]
  implicit def setEncoder[T, Cas](implicit mapped: MappedType[T, Cas]): Encoder[Set[T]] = encoder[Set[T]]

  implicit def mapDecoder[K: ClassTag, V: ClassTag, KCas: ClassTag, VCas: ClassTag](
    implicit
    km: MappedType[K, KCas],
    vm: MappedType[V, VCas]
  ): Decoder[Map[K, V]] = decoder[Map[K, V]]
  implicit def mapEncoder[K, V, KCas, VCas](implicit km: MappedType[K, KCas], vm: MappedType[V, VCas]): Encoder[Map[K, V]] = encoder[Map[K, V]]
}