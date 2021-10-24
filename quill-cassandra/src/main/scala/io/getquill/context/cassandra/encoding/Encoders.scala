package io.getquill.context.cassandra.encoding

import io.getquill.context.cassandra.CassandraRowContext

import java.nio.ByteBuffer
import java.time.{ Instant, LocalDate, LocalTime }
import java.util.{ Date, UUID }

trait Encoders extends CollectionEncoders {
  this: CassandraRowContext[_] =>

  type Encoder[T] = CassandraEncoder[T]

  case class CassandraEncoder[T](encoder: BaseEncoder[T]) extends BaseEncoder[T] {
    override def apply(index: Index, value: T, row: PrepareRow, session: Session) =
      encoder(index, value, row, session)
  }

  def encoder[T](e: BaseEncoder[T]): Encoder[T] = CassandraEncoder(e)

  def encoder[T](f: PrepareRow => (Index, T) => PrepareRow): Encoder[T] =
    encoder((index, value, row, _) => f(row)(index, value))

  private[this] val nullEncoder: Encoder[Null] =
    encoder((index, _, row, _) => row.setToNull(index))

  implicit def optionEncoder[T](implicit d: Encoder[T]): Encoder[Option[T]] =
    encoder { (index, value, row, session) =>
      value match {
        case None    => nullEncoder(index, null, row, session)
        case Some(v) => d(index, v, row, session)
      }
    }

  implicit def mappedEncoder[I, O](implicit mapped: MappedEncoding[I, O], encoder: Encoder[O]): Encoder[I] =
    CassandraEncoder(mappedBaseEncoder(mapped, encoder.encoder))

  implicit val stringEncoder: Encoder[String] = encoder(_.setString)
  implicit val bigDecimalEncoder: Encoder[BigDecimal] =
    encoder((index, value, row, _) => row.setBigDecimal(index, value.bigDecimal))
  implicit val booleanEncoder: Encoder[Boolean] = encoder(_.setBoolean)
  implicit val byteEncoder: Encoder[Byte] = encoder(_.setByte)
  implicit val shortEncoder: Encoder[Short] = encoder(_.setShort)
  implicit val intEncoder: Encoder[Int] = encoder(_.setInt)
  implicit val longEncoder: Encoder[Long] = encoder(_.setLong)
  implicit val floatEncoder: Encoder[Float] = encoder(_.setFloat)
  implicit val doubleEncoder: Encoder[Double] = encoder(_.setDouble)
  implicit val byteArrayEncoder: Encoder[Array[Byte]] =
    encoder((index, value, row, _) => row.setByteBuffer(index, ByteBuffer.wrap(value)))
  implicit val uuidEncoder: Encoder[UUID] = encoder(_.setUuid)
  implicit val timestampEncoder: Encoder[Instant] = encoder(_.setInstant)
  implicit val cassandraLocalTimeEncoder: Encoder[LocalTime] = encoder(_.setLocalTime)
  implicit val cassandraLocalDateEncoder: Encoder[LocalDate] = encoder(_.setLocalDate)
}
