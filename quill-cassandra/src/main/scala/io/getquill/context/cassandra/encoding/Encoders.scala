package io.getquill.context.cassandra.encoding

import java.nio.ByteBuffer
import java.util.{ Date, UUID }

import io.getquill.context.cassandra.CassandraSessionContext

import scala.collection.JavaConverters._
import scala.reflect.ClassTag

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

  trait CollectionItemEncoder {
    def apply[T <: AnyRef: ClassTag](value: java.util.Set[T]): Unit
  }

  sealed trait CollectionItemEncodingType[I] {
    def apply(value: Set[I], converter: CollectionItemEncoder): Unit
  }

  object CollectionItemEncodingType {

    abstract class ItemType[I] extends CollectionItemEncodingType[I] {
      def apply[O <: AnyRef: ClassTag](value: Set[I], converter: CollectionItemEncoder)(implicit f: I => O): Unit =
        converter[O](value.map(f).asJava)
    }

    implicit object Int extends ItemType[Int] {
      override def apply(value: Set[Int], converter: CollectionItemEncoder) =
        apply[java.lang.Integer](value, converter)
    }

    implicit object Long extends ItemType[Long] {
      override def apply(value: Set[Long], converter: CollectionItemEncoder) =
        apply[java.lang.Long](value, converter)
    }

  }

  case class CollectionItemRowEncoder(index: Index, r: PrepareRow) extends CollectionItemEncoder {
    override def apply[T <: AnyRef: ClassTag](value: java.util.Set[T]) = {
      val _ = r.setSet(index, value, implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]])
    }
  }

  implicit def setEncoder[I](implicit cit: CollectionItemEncodingType[I]): Encoder[Set[I]] =
    encoder((index, value, row) => {
      cit(value, CollectionItemRowEncoder(index, row))
      row
    })

}
