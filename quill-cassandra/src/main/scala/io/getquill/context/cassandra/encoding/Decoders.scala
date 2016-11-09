package io.getquill.context.cassandra.encoding

import io.getquill.context.cassandra.CassandraSessionContext
import io.getquill.util.Messages.fail

trait Decoders {
  this: CassandraSessionContext[_] =>

  type Decoder[T] = CassandraDecoder[T]

  case class CassandraDecoder[T](decoder: BaseDecoder[T]) extends BaseDecoder[T] {
    override def apply(index: Index, row: ResultRow) =
      decoder(index, row)
  }

  def decoder[T](d: BaseDecoder[T]): Decoder[T] = CassandraDecoder(
    (index, row) => {
      row.isNull(index) match {
        case true  => fail(s"Expected column at index $index to be defined but is was empty")
        case false => d(index, row)
      }
    }
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
}
