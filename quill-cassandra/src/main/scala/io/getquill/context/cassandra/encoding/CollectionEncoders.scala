package io.getquill.context.cassandra.encoding

import com.datastax.oss.driver.api.core.`type`.reflect.GenericType
import com.datastax.oss.driver.shaded.guava.common.reflect.{ TypeParameter, TypeToken }
import io.getquill.context.cassandra.CassandraRowContext
import io.getquill.context.cassandra.util.ClassTagConversions.asClassOf

import scala.jdk.CollectionConverters._

trait CollectionEncoders {
  this: CassandraRowContext[_] =>
  import scala.reflect._

  implicit def listEncoder[T, Cas: ClassTag](implicit mapper: CassandraMapper[T, Cas]): Encoder[List[T]] = {
    encoder((index, list, row, session) => row.setList[Cas](index.toInt, list.map(row => mapper.f(row, session)).asJava, asClassOf[Cas]))
  }

  implicit def setEncoder[T, Cas: ClassTag](implicit mapper: CassandraMapper[T, Cas]): Encoder[Set[T]] =
    encoder((index, list, row, session) => row.setSet[Cas](index.toInt, list.map(row => mapper.f(row, session)).asJava, asClassOf[Cas]))

  implicit def mapEncoder[K, V, KCas: ClassTag, VCas: ClassTag](
    implicit
    keyMapper: CassandraMapper[K, KCas],
    valMapper: CassandraMapper[V, VCas]
  ): Encoder[Map[K, V]] =
    encoder((index, map, row, session) => row.setMap(index, map
      .map(kv => keyMapper.f(kv._1, session) -> valMapper.f(kv._2, session)).asJava, asClassOf[KCas], asClassOf[VCas]))
}
