package io.getquill

import java.util.Date

import com.datastax.driver.core.LocalDate
import io.getquill.context.cassandra.encoding.{ CassandraMapper, CassandraType }
import io.getquill.context.cassandra.{ CassandraContext, CqlIdiom, Udt }

import scala.reflect.ClassTag

class CassandraMirrorContextWithQueryProbing extends CassandraMirrorContext(Literal) with QueryProbing

class CassandraMirrorContext[Naming <: NamingStrategy](naming: Naming)
  extends MirrorContext[CqlIdiom, Naming](CqlIdiom, naming) with CassandraContext[Naming] {

  implicit val timestampDecoder: Decoder[Date] = decoder[Date]
  implicit val timestampEncoder: Encoder[Date] = encoder[Date]
  implicit val cassandraLocalDateDecoder: Decoder[LocalDate] = decoder[LocalDate]
  implicit val cassandraLocalDateEncoder: Encoder[LocalDate] = encoder[LocalDate]

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

  implicit def udtCassandraType[T <: Udt]: CassandraType[T] = CassandraType.of[T]
  implicit def udtDecoder[T <: Udt: ClassTag]: Decoder[T] = decoder[T]
  implicit def udtEncoder[T <: Udt]: Encoder[T] = encoder[T]
}