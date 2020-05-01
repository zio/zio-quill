package io.getquill.context.cassandra.encoding

import io.getquill.context.cassandra.CassandraSessionContext

import scala.jdk.CollectionConverters._

trait CollectionEncoders {
  this: CassandraSessionContext[_] =>

  implicit def listEncoder[T, Cas](implicit mapper: CassandraMapper[T, Cas]): Encoder[List[T]] =
    encoder((index, list, row) => row.setList[Cas](index, list.map(mapper.f).asJava))

  implicit def setEncoder[T, Cas](implicit mapper: CassandraMapper[T, Cas]): Encoder[Set[T]] =
    encoder((index, set, row) => row.setSet[Cas](index, set.map(mapper.f).asJava))

  implicit def mapEncoder[K, V, KCas, VCas](
    implicit
    keyMapper: CassandraMapper[K, KCas],
    valMapper: CassandraMapper[V, VCas]
  ): Encoder[Map[K, V]] = encoder((index, map, row) => row.setMap[KCas, VCas](index, map
    .map(kv => keyMapper.f(kv._1) -> valMapper.f(kv._2)).asJava))
}
