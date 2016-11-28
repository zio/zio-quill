package io.getquill.context.cassandra.encoding

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

  implicit val stringDecoder = decoder(_.getString)
  implicit val bigDecimalDecoder: Decoder[BigDecimal] =
    decoder((index, row) => row.getDecimal(index))
  implicit val booleanDecoder = decoder(_.getBool)
  implicit val intDecoder = decoder(_.getInt)
  implicit val longDecoder = decoder(_.getLong)
  implicit val floatDecoder = decoder(_.getFloat)
  implicit val doubleDecoder = decoder(_.getDouble)
  implicit val byteArrayDecoder =
    decoder((index, row) => {
      val bb = row.getBytes(index)
      val b = new Array[Byte](bb.remaining())
      bb.get(b)
      b
    })
  implicit val uuidDecoder = decoder(_.getUUID)
  implicit val dateDecoder = decoder(_.getTimestamp)

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

    implicit object Int extends ItemType[Int] {
      override def apply(converter: CollectionItemDecoder) =
        apply[java.lang.Integer](converter)
    }

    implicit object Long extends ItemType[Long] {
      override def apply(converter: CollectionItemDecoder) =
        apply[java.lang.Long](converter)
    }

  }

  case class CollectionItemRowDecoder(index: Index, r: ResultRow) extends CollectionItemDecoder {
    override def apply[T <: AnyRef: ClassTag](): java.util.Set[T] =
      r.getSet(index, implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]])
  }

  implicit def setDecoder[I: TypeTag](implicit cit: CollectionItemDecodingType[I], t: ClassTag[I]): Decoder[Set[I]] =
    decoder((index, row) =>
      Option(cit.apply(CollectionItemRowDecoder(index, row))).getOrElse(Set()))
}
