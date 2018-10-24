package io.getquill.context.orientdb.encoding

import java.util.Date

import io.getquill.context.orientdb.OrientDBSessionContext

trait Encoders extends CollectionEncoders {
  this: OrientDBSessionContext[_] =>

  type Encoder[T] = OrientDBEncoder[T]

  case class OrientDBEncoder[T](encoder: BaseEncoder[T]) extends BaseEncoder[T] {
    override def apply(index: Index, value: T, row: PrepareRow) =
      encoder(index, value, row)
  }

  def encoder[T](e: BaseEncoder[T]): Encoder[T] = OrientDBEncoder(e)

  def encoder[T](f: PrepareRow => (Index, T) => PrepareRow): Encoder[T] =
    encoder((index, value, row) => f(row)(index, value))

  private[this] val nullEncoder: Encoder[Null] =
    encoder((index, value, row) => { row.insert(index, null); row })

  implicit def optionEncoder[T](implicit d: Encoder[T]): Encoder[Option[T]] =
    encoder { (index, value, row) =>
      value match {
        case None    => nullEncoder(index, null, row)
        case Some(v) => d(index, v, row)
      }
    }

  implicit def mappedEncoder[I, O](implicit mapped: MappedEncoding[I, O], encoder: Encoder[O]): Encoder[I] =
    OrientDBEncoder(mappedBaseEncoder(mapped, encoder.encoder))

  implicit val stringEncoder: Encoder[String] = encoder((index, value, row) => { row.insert(index, value); row })
  implicit val bigDecimalEncoder: Encoder[BigDecimal] = encoder((index, value, row) => { row.insert(index, value.bigDecimal); row })
  implicit val booleanEncoder: Encoder[Boolean] = encoder((index, value, row) => { row.insert(index, value); row })
  implicit val intEncoder: Encoder[Int] = encoder((index, value, row) => { row.insert(index, value); row })
  implicit val shortEncoder: Encoder[Short] = encoder((index, value, row) => { row.insert(index, value); row })
  implicit val byteEncoder: Encoder[Byte] = encoder((index, value, row) => { row.insert(index, value); row })
  implicit val longEncoder: Encoder[Long] = encoder((index, value, row) => { row.insert(index, value); row })
  implicit val floatEncoder: Encoder[Float] = encoder((index, value, row) => { row.insert(index, value); row })
  implicit val doubleEncoder: Encoder[Double] = encoder((index, value, row) => { row.insert(index, value); row })
  implicit val dateEncoder: Encoder[Date] = encoder((index, value, row) => { row.insert(index, value); row })
  implicit val byteArrayEncoder: Encoder[Array[Byte]] = encoder((index, value, row) => { row.insert(index, value); row })
}