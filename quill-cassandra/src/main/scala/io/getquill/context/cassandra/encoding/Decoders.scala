package io.getquill.context.cassandra.encoding

import com.datastax.oss.driver.internal.core.`type`.{ DefaultListType, PrimitiveType }
import io.getquill.context.cassandra.CassandraRowContext
import io.getquill.util.Messages.fail

import java.time.{ Instant, LocalDate, LocalTime }
import java.util.{ Date, UUID }

trait Decoders extends CollectionDecoders {
  this: CassandraRowContext[_] =>

  type Decoder[T] = CassandraDecoder[T]

  case class CassandraDecoder[T](decoder: BaseDecoder[T]) extends BaseDecoder[T] {
    override def apply(index: Index, row: ResultRow, session: Session) =
      decoder(index, row, session)
  }

  def decoder[T](d: BaseDecoder[T]): Decoder[T] = CassandraDecoder(
    (index, row, session) =>
      if (row.isNull(index) && row.getColumnDefinitions.get(index).getType.isInstanceOf[PrimitiveType])
        fail(s"Expected column at index $index to be defined but is was empty or type is unknown ${row.getColumnDefinitions.get(index).getType.getClass}")
      else d(index, row, session)

  )

  def decoder[T](f: ResultRow => Index => T): Decoder[T] =
    decoder((index, row, session) => f(row)(index))

  implicit def optionDecoder[T](implicit d: Decoder[T]): Decoder[Option[T]] =
    CassandraDecoder((index, row, session) => {
      row.isNull(index) match {
        case true  => None
        case false => Some(d(index, row, session))
      }
    })

  implicit def mappedDecoder[I, O](implicit mapped: MappedEncoding[I, O], decoder: Decoder[I]): Decoder[O] =
    CassandraDecoder(mappedBaseDecoder(mapped, decoder.decoder))

  implicit val stringDecoder: Decoder[String] = decoder(_.getString)
  implicit val bigDecimalDecoder: Decoder[BigDecimal] =
    decoder((index, row, session) => row.getBigDecimal(index))
  implicit val booleanDecoder: Decoder[Boolean] = decoder(_.getBoolean)
  implicit val byteDecoder: Decoder[Byte] = decoder(_.getByte)
  implicit val shortDecoder: Decoder[Short] = decoder(_.getShort)
  implicit val intDecoder: Decoder[Int] = decoder(_.getInt)
  implicit val longDecoder: Decoder[Long] = decoder(_.getLong)
  implicit val floatDecoder: Decoder[Float] = decoder(_.getFloat)
  implicit val doubleDecoder: Decoder[Double] = decoder(_.getDouble)
  implicit val byteArrayDecoder: Decoder[Array[Byte]] =
    decoder((index, row, session) => {
      val bb = row.getByteBuffer(index)
      val b = new Array[Byte](bb.remaining())
      bb.get(b)
      b
    })
  implicit val uuidDecoder: Decoder[UUID] = decoder(_.getUuid)
  implicit val timestampDecoder: Decoder[Instant] = decoder(_.getInstant)
  implicit val cassandraLocalTimeDecoder: Decoder[LocalTime] = decoder(_.getLocalTime)
  implicit val cassandraLocalDateDecoder: Decoder[LocalDate] = decoder(_.getLocalDate)
}
