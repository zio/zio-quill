package io.getquill

import io.getquill.context.cassandra.encoding.CassandraMapper
import io.getquill.context.cassandra.{ CassandraContext, CqlIdiom }

import scala.reflect.ClassTag

class CassandraMirrorContextWithQueryProbing extends CassandraMirrorContext with QueryProbing

class CassandraMirrorContext[Naming <: NamingStrategy]
  extends MirrorContext[CqlIdiom, Naming] with CassandraContext[Naming] {

  implicit def listDecoder[T: ClassTag, Cas: ClassTag](implicit mapper: CassandraMapper[Cas, T]): Decoder[List[T]] = decoder[List[T]]
  implicit def setDecoder[T: ClassTag, Cas: ClassTag](implicit mapper: CassandraMapper[Cas, T]): Decoder[Set[T]] = decoder[Set[T]]
  implicit def mapDecoder[K: ClassTag, V: ClassTag, KCas: ClassTag, VCas: ClassTag](
    implicit
    keyMapper: CassandraMapper[KCas, K],
    valMapper: CassandraMapper[VCas, V]
  ): Decoder[Map[K, V]] = decoder[Map[K, V]]

  implicit def listEncoder[T, Cas](implicit mapper: CassandraMapper[T, Cas]): Encoder[List[T]] = encoder[List[T]]
  implicit def setEncoder[T, Cas](implicit mapper: CassandraMapper[T, Cas]): Encoder[Set[T]] = encoder[Set[T]]
  implicit def mapEncoder[K, V, KCas, VCas](
    implicit
    keyMapper: CassandraMapper[K, KCas],
    valMapper: CassandraMapper[V, VCas]
  ): Encoder[Map[K, V]] = encoder[Map[K, V]]
}