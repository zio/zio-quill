package io.getquill.context.cassandra.encoding

import java.util.{ Date, UUID }

import io.getquill.context.cassandra.CassandraSessionContext
import io.getquill.util.Messages.fail

import scala.collection.JavaConverters._
import scala.reflect.ClassTag
import scala.reflect.runtime.universe.{ TypeTag, typeOf }

trait Decoders {
  this: CassandraSessionContext[_] =>

  type Decoder[T] = CassandraDecoder[T]

  case class CassandraDecoder[T](decoder: BaseDecoder[T]) extends BaseDecoder[T] {
    override def apply(index: Index, row: ResultRow) =
      decoder(index, row)
  }

  def decoder[T: TypeTag](d: BaseDecoder[T]): Decoder[T] = CassandraDecoder(
    (index, row) => {
      if (typeOf[T] <:< typeOf[Set[_]] || !row.isNull(index)) {
        d(index, row)
      } else {
        fail(s"Expected column at index $index to be defined but is was empty")
      }
    }
  )

  def decoder[T: TypeTag](f: ResultRow => Index => T): Decoder[T] =
    decoder((index, row) => f(row)(index))

  implicit def optionDecoder[T](implicit d: Decoder[T]): Decoder[Option[T]] =
    CassandraDecoder((index, row) => {
      if (row.isNull(index)) {
        None
      } else {
        Some(d(index, row))
      }
    })

  implicit def mappedDecoder[I, O](implicit mapped: MappedEncoding[I, O], decoder: Decoder[I]): Decoder[O] =
    CassandraDecoder(mappedBaseDecoder(mapped, decoder.decoder))

  implicit val stringDecoder: Decoder[String] = decoder(_.getString)
  implicit val bigDecimalDecoder: Decoder[BigDecimal] =
    decoder((index, row) => row.getDecimal(index))
  implicit val booleanDecoder: Decoder[Boolean] = decoder(_.getBool)
  implicit val intDecoder: Decoder[Int] = decoder(_.getInt)
  implicit val longDecoder: Decoder[Long] = decoder(_.getLong)
  implicit val floatDecoder: Decoder[Float] = decoder(_.getFloat)
  implicit val doubleDecoder: Decoder[Double] = decoder(_.getDouble)
  implicit val byteArrayDecoder: Decoder[Array[Byte]] =
    decoder((index, row) => {
      val bb = row.getBytes(index)
      val b = new Array[Byte](bb.remaining())
      bb.get(b)
      b
    })
  implicit val uuidDecoder: Decoder[UUID] = decoder(_.getUUID)
  implicit val dateDecoder: Decoder[Date] = decoder(_.getTimestamp)

  trait CollectionItemDecoder {
    def apply[T <: AnyRef: ClassTag](): java.util.Set[T]
  }

  sealed trait CollectionItemDecodingType[I] {
    def apply(converter: CollectionItemDecoder): Set[I]
  }

  object CollectionItemDecodingType {

    abstract class ItemType[I] extends CollectionItemDecodingType[I] {
      def apply[O <: AnyRef: ClassTag](converter: CollectionItemDecoder)(implicit f: O => I): Set[I] =
        converter[O]().asScala.map(f).toSet
    }

    class RefItemType[I <: AnyRef: ClassTag] extends ItemType[I] {
      override def apply(converter: CollectionItemDecoder) =
        apply[I](converter)
    }

    implicit object Int extends ItemType[Int] {
      override def apply(converter: CollectionItemDecoder) =
        apply[java.lang.Integer](converter)
    }

    implicit object Long extends ItemType[Long] {
      override def apply(converter: CollectionItemDecoder) =
        apply[java.lang.Long](converter)
    }

    implicit object String extends RefItemType[String]

  }

  case class CollectionItemRowDecoder(index: Index, r: ResultRow) extends CollectionItemDecoder {
    override def apply[T <: AnyRef: ClassTag](): java.util.Set[T] =
      r.getSet(index, implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]])
  }

  implicit def setMappedDecoder[O: TypeTag, I](implicit mapped: MappedEncoding[I, O], cit: CollectionItemDecodingType[I]): Decoder[Set[O]] =
    decoder((index, row) =>
      Option(cit.apply(CollectionItemRowDecoder(index, row))).getOrElse(Set()).map(mapped.f))

  implicit def setDecoder[O: TypeTag](implicit cit: CollectionItemDecodingType[O]): Decoder[Set[O]] = {
    implicit val mapped = MappedEncoding[O, O](identity)
    setMappedDecoder[O, O]
  }
}
