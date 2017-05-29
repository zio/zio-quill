package io.getquill

import io.getquill.context.cassandra.encoding.CassandraMapper
import io.getquill.context.cassandra.{ CassandraContext, CqlIdiom }

import scala.reflect.ClassTag

class CassandraMirrorContextWithQueryProbing extends CassandraMirrorContext with QueryProbing

class CassandraMirrorContext[Naming <: NamingStrategy]
  extends MirrorContext[CqlIdiom, Naming] with CassandraContext[Naming] {

  implicit def listDecoder[T, Cas: ClassTag](implicit mapper: CassandraMapper[Cas, T]): Decoder[List[T]] = decoderUnsafe[List[T]]
  implicit def setDecoder[T, Cas: ClassTag](implicit mapper: CassandraMapper[Cas, T]): Decoder[Set[T]] = decoderUnsafe[Set[T]]
  implicit def mapDecoder[K, V, KCas: ClassTag, VCas: ClassTag](
    implicit
    keyMapper: CassandraMapper[KCas, K],
    valMapper: CassandraMapper[VCas, V]
  ): Decoder[Map[K, V]] = decoderUnsafe[Map[K, V]]

  implicit def listEncoder[T, Cas](implicit mapper: CassandraMapper[T, Cas]): Encoder[List[T]] = encoder[List[T]]
  implicit def setEncoder[T, Cas](implicit mapper: CassandraMapper[T, Cas]): Encoder[Set[T]] = encoder[Set[T]]
  implicit def mapEncoder[K, V, KCas, VCas](
    implicit
    keyMapper: CassandraMapper[K, KCas],
    valMapper: CassandraMapper[V, VCas]
  ): Encoder[Map[K, V]] = encoder[Map[K, V]]
}