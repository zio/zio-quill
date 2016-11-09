package io.getquill.context.cassandra.encoding

import java.nio.ByteBuffer

import io.getquill.context.cassandra.CassandraSessionContext

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

  implicit val stringEncoder = encoder(_.setString)
  implicit val bigDecimalEncoder: Encoder[BigDecimal] =
    encoder((index, value, row) => row.setDecimal(index, value.bigDecimal))
  implicit val booleanEncoder = encoder(_.setBool)
  implicit val intEncoder = encoder(_.setInt)
  implicit val longEncoder = encoder(_.setLong)
  implicit val floatEncoder = encoder(_.setFloat)
  implicit val doubleEncoder = encoder(_.setDouble)
  implicit val byteArrayEncoder: Encoder[Array[Byte]] =
    encoder((index, value, row) => row.setBytes(index, ByteBuffer.wrap(value)))
  implicit val uuidEncoder = encoder(_.setUUID)
  implicit val dateEncoder = encoder(_.setTimestamp)
}
