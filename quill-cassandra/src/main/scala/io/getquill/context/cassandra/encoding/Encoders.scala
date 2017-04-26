package io.getquill.context.cassandra.encoding

import java.nio.ByteBuffer
import java.util.{ Date, UUID }

import com.datastax.driver.core.LocalDate
import io.getquill.context.cassandra.{ CassandraSessionContext, MappedType }

import scala.collection.JavaConverters._

trait Encoders {
  this: CassandraSessionContext[_] =>

  type Encoder[T] = CassandraEncoder[T]

  case class CassandraEncoder[T](encoder: BaseEncoder[T]) extends BaseEncoder[T] {
    override def apply(index: Index, value: T, row: PrepareRow) =
      encoder(index, value, row)
  }

  def encoder[T](e: BaseEncoder[T]): Encoder[T] = CassandraEncoder(e)

  def encoder[T](f: PrepareRow => (Index, T) => PrepareRow): Encoder[T] =
    encoder((index, value, row) => f(row)(index, value))

  private[this] val nullEncoder: Encoder[Null] =
    encoder((index, value, row) => row.setToNull(index))

  implicit def optionEncoder[T](implicit d: Encoder[T]): Encoder[Option[T]] =
    encoder { (index, value, row) =>
      value match {
        case None    => nullEncoder(index, null, row)
        case Some(v) => d(index, v, row)
      }
    }

  implicit def mappedEncoder[I, O](implicit mapped: MappedEncoding[I, O], encoder: Encoder[O]): Encoder[I] =
    CassandraEncoder(mappedBaseEncoder(mapped, encoder.encoder))

  implicit val stringEncoder: Encoder[String] = encoder(_.setString)
  implicit val bigDecimalEncoder: Encoder[BigDecimal] =
    encoder((index, value, row) => row.setDecimal(index, value.bigDecimal))
  implicit val booleanEncoder: Encoder[Boolean] = encoder(_.setBool)
  implicit val intEncoder: Encoder[Int] = encoder(_.setInt)
  implicit val longEncoder: Encoder[Long] = encoder(_.setLong)
  implicit val floatEncoder: Encoder[Float] = encoder(_.setFloat)
  implicit val doubleEncoder: Encoder[Double] = encoder(_.setDouble)
  implicit val byteArrayEncoder: Encoder[Array[Byte]] =
    encoder((index, value, row) => row.setBytes(index, ByteBuffer.wrap(value)))
  implicit val uuidEncoder: Encoder[UUID] = encoder(_.setUUID)
  implicit val dateEncoder: Encoder[Date] = encoder(_.setTimestamp)
  implicit val localDateEncoder: Encoder[LocalDate] = encoder(_.setDate)

  implicit def listEncoder[T, Cas](implicit mapped: MappedType[T, Cas]): Encoder[List[T]] =
    encoder((index, list, row) => row.setList[Cas](index, list.map(mapped.encode).asJava))

  implicit def setEncoder[T, Cas](implicit mapped: MappedType[T, Cas]): Encoder[Set[T]] =
    encoder((index, set, row) => row.setSet[Cas](index, set.map(mapped.encode).asJava))

  implicit def mapEncoder[K, V, KCas, VCas](
    implicit
    km: MappedType[K, KCas],
    vm: MappedType[V, VCas]
  ): Encoder[Map[K, V]] =
    encoder((index, map, row) => row.setMap[KCas, VCas](index, map.map(kv => (km.encode(kv._1), vm.encode(kv._2))).asJava))
}
