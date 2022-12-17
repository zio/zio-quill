package io.getquill.context.cassandra.encoding

import io.getquill.context.cassandra.CassandraRowContext
import io.getquill.context.cassandra.util.ClassTagConversions.asClassOf

import scala.jdk.CollectionConverters._
import scala.reflect.ClassTag

trait CollectionDecoders {
  this: CassandraRowContext[_] =>

  implicit def listDecoder[T, Cas: ClassTag](implicit mapper: CassandraMapper[Cas, T]): Decoder[List[T]] =
    decoder((index, row, session) => row.getList[Cas](index, asClassOf[Cas]).asScala.map(row => mapper.f(row, session)).toList)

  implicit def setDecoder[T, Cas: ClassTag](implicit mapper: CassandraMapper[Cas, T]): Decoder[Set[T]] =
    decoder((index, row, session) => row.getSet[Cas](index, asClassOf[Cas]).asScala.map(row => mapper.f(row, session)).toSet)

  implicit def mapDecoder[K, V, KCas: ClassTag, VCas: ClassTag](
    implicit
    keyMapper: CassandraMapper[KCas, K],
    valMapper: CassandraMapper[VCas, V]
  ): Decoder[Map[K, V]] = decoder((index, row, session) => row.getMap[KCas, VCas](index, asClassOf[KCas], asClassOf[VCas])
    .asScala.map(kv => keyMapper.f(kv._1, session) -> valMapper.f(kv._2, session)).toMap)

}
