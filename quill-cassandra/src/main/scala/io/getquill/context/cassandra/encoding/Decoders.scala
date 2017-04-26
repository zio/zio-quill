package io.getquill.context.cassandra.encoding

import io.getquill.context.cassandra.{ CassandraSessionContext, MappedType }
import io.getquill.util.Messages.fail
import scala.collection.JavaConverters._

trait Decoders {
  this: CassandraSessionContext[_] =>

  type Decoder[T] = CassandraDecoder[T]

  case class CassandraDecoder[T](decoder: BaseDecoder[T]) extends BaseDecoder[T] {
    override def apply(index: Index, row: ResultRow) =
      decoder(index, row)
  }

  def decoder[T](d: BaseDecoder[T]): Decoder[T] = CassandraDecoder(
    (index, row) =>
      if (row.isNull(index) && !row.getColumnDefinitions.getType(index).isCollection)
        fail(s"Expected column at index $index to be defined but is was empty")
      else d(index, row)

  )

  def decoder[T](f: ResultRow => Index => T): Decoder[T] =
    decoder((index, row) => f(row)(index))

  implicit def optionDecoder[T](implicit d: Decoder[T]): Decoder[Option[T]] =
    CassandraDecoder((index, row) => {
      row.isNull(index) match {
        case true  => None
        case false => Some(d(index, row))
      }
    })

  implicit def mappedDecoder[I, O](implicit mapped: MappedEncoding[I, O], decoder: Decoder[I]): Decoder[O] =
    CassandraDecoder(mappedBaseDecoder(mapped, decoder.decoder))

  implicit def genericDecoder[T, Cas](implicit mapped: MappedType[T, Cas]): Decoder[T] =
    decoder((index, row) => mapped.decode(row.get[Cas](index, mapped.codec)))

  implicit def listDecoder[T, Cas](implicit mapped: MappedType[T, Cas]): Decoder[List[T]] =
    decoder((index, row) => row.getList[Cas](index, mapped.codec.getJavaType).asScala.map(mapped.decode).toList)

  implicit def setDecoder[T, Cas](implicit mapped: MappedType[T, Cas]): Decoder[Set[T]] =
    decoder((index, row) => row.getSet[Cas](index, mapped.codec.getJavaType).asScala.map(mapped.decode).toSet)

  implicit def mapDecoder[K, V, KCas, VCas](
    implicit
    km: MappedType[K, KCas],
    vm: MappedType[V, VCas]
  ): Decoder[Map[K, V]] =
    decoder((index, row) => row.getMap[KCas, VCas](index, km.codec.getJavaType, vm.codec.getJavaType).asScala.map {
      case (k, v) => km.decode(k) -> vm.decode(v)
    }.toMap)
}
