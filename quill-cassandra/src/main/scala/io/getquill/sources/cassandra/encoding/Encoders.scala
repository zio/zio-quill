package io.getquill.sources.cassandra.encoding

import java.util.UUID

import com.datastax.driver.core.BoundStatement
import java.nio.ByteBuffer
import io.getquill.sources.cassandra.CassandraSource
import com.datastax.driver.core.Row

trait Encoders {
  this: CassandraSource[_, Row, BoundStatement] =>

  private def encoder[T](f: BoundStatement => (Int, T) => BoundStatement): Encoder[T] =
    new Encoder[T] {
      override def apply(index: Int, value: T, row: BoundStatement) =
        f(row)(index, value)
    }

  implicit def optionEncoder[T](implicit d: Encoder[T]): Encoder[Option[T]] =
    new Encoder[Option[T]] {
      override def apply(index: Int, value: Option[T], row: BoundStatement) =
        value match {
          case None        => row.setToNull(index)
          case Some(value) => d(index, value, row)
        }
    }

  implicit val stringEncoder = encoder(_.setString)
  implicit val bigDecimalEncoder: Encoder[BigDecimal] = new Encoder[BigDecimal] {
    override def apply(index: Int, value: BigDecimal, row: BoundStatement) =
      row.setDecimal(index, value.bigDecimal)
  }
  implicit val booleanEncoder = encoder(_.setBool)
  implicit val intEncoder = encoder(_.setInt)
  implicit val longEncoder = encoder(_.setLong)
  implicit val floatEncoder = encoder(_.setFloat)
  implicit val doubleEncoder = encoder(_.setDouble)
  implicit val byteArrayEncoder: Encoder[Array[Byte]] = new Encoder[Array[Byte]] {
    override def apply(index: Int, value: Array[Byte], row: BoundStatement) =
      row.setBytes(index, ByteBuffer.wrap(value))
  }
  implicit val uuidEncoder: Encoder[UUID] = new Encoder[UUID] {
    override def apply(index: Int, value: UUID, row: BoundStatement) =
      row.setUUID(index,value)
  }
  implicit val dateEncoder = encoder(_.setDate)
}
