package io.getquill.context.cassandra.encoding

import java.util.{ Date, UUID }

import com.datastax.driver.core.LocalDate
import io.getquill.context.cassandra.{ CassandraSessionContext, MappedType }
import io.getquill.util.Messages.fail

import scala.collection.JavaConverters._
import scala.reflect.ClassTag

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
  implicit val localDateDecoder: Decoder[LocalDate] = decoder(_.getDate)

  implicit def listDecoder[T: ClassTag, Cas: ClassTag](implicit mapped: MappedType[T, Cas]): Decoder[List[T]] =
    decoder((index, row) => row.getList[Cas](index, javaClass[Cas]).asScala.map(mapped.decode).toList)

  implicit def setDecoder[T: ClassTag, Cas: ClassTag](implicit mapped: MappedType[T, Cas]): Decoder[Set[T]] =
    decoder((index, row) => row.getSet[Cas](index, javaClass[Cas]).asScala.map(mapped.decode).toSet)

  implicit def mapDecoder[K: ClassTag, V: ClassTag, KCas: ClassTag, VCas: ClassTag](
    implicit
    km: MappedType[K, KCas],
    vm: MappedType[V, VCas]
  ): Decoder[Map[K, V]] =
    decoder((index, row) => row.getMap[KCas, VCas](index, javaClass[KCas], javaClass[VCas]).asScala.map {
      case (k, v) => km.decode(k) -> vm.decode(v)
    }.toMap)

  private def javaClass[T](implicit tag: ClassTag[T]): Class[T] = tag.runtimeClass.asInstanceOf[Class[T]]
}
