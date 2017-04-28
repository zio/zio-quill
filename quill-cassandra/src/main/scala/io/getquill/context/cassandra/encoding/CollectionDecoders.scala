package io.getquill.context.cassandra.encoding

import io.getquill.context.cassandra.CassandraSessionContext
import io.getquill.context.cassandra.util.ClassTagConversions.asClassOf

import scala.reflect.ClassTag
import scala.collection.JavaConverters._

trait CollectionDecoders {
  this: CassandraSessionContext[_] =>

  implicit def listDecoder[T, Cas: ClassTag](implicit mapper: CassandraMapper[Cas, T]): Decoder[List[T]] =
    decoder((index, row) => row.getList[Cas](index, asClassOf[Cas]).asScala.map(mapper.f).toList)

  implicit def setDecoder[T, Cas: ClassTag](implicit mapper: CassandraMapper[Cas, T]): Decoder[Set[T]] =
    decoder((index, row) => row.getSet[Cas](index, asClassOf[Cas]).asScala.map(mapper.f).toSet)

  implicit def mapDecoder[K, V, KCas: ClassTag, VCas: ClassTag](
    implicit
    keyMapper: CassandraMapper[KCas, K],
    valMapper: CassandraMapper[VCas, V]
  ): Decoder[Map[K, V]] = decoder((index, row) => row.getMap[KCas, VCas](index, asClassOf[KCas], asClassOf[VCas])
    .asScala.map(kv => keyMapper.f(kv._1) -> valMapper.f(kv._2)).toMap)

}
