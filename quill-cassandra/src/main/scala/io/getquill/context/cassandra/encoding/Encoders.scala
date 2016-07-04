package io.getquill.context.cassandra.encoding

import java.util.UUID
import com.datastax.driver.core.BoundStatement
import java.nio.ByteBuffer
import io.getquill.context.cassandra.CassandraContext
import com.datastax.driver.core.Row
import io.getquill.context.BindedStatementBuilder

trait Encoders {
  this: CassandraContext[_, Row, BindedStatementBuilder[BoundStatement]] =>

  def encoder[T](f: BoundStatement => (Int, T) => BoundStatement): Encoder[T] =
    new Encoder[T] {
      override def apply(idx: Int, value: T, row: BindedStatementBuilder[BoundStatement]) = {
        val raw = new io.getquill.context.Encoder[BoundStatement, T] {
          override def apply(idx: Int, value: T, row: BoundStatement) =
            f(row)(idx, value)
        }
        row.single(idx, value, raw)
      }
    }

  implicit def traversableEncoder[T](implicit e: Encoder[T]): Encoder[Traversable[T]] =
    new Encoder[Traversable[T]] {
      override def apply(idx: Int, values: Traversable[T], row: BindedStatementBuilder[BoundStatement]) =
        row.coll(idx, values, e)
    }

  private[this] val nullEncoder = encoder[Null] { row => (idx, v) =>
    row.setToNull(idx)
  }

  implicit def optionEncoder[T](implicit d: Encoder[T]): Encoder[Option[T]] =
    new Encoder[Option[T]] {
      override def apply(idx: Int, value: Option[T], row: BindedStatementBuilder[BoundStatement]) =
        value match {
          case None    => nullEncoder(idx, null, row)
          case Some(v) => d(idx, v, row)
        }
    }

  implicit val stringEncoder = encoder(_.setString)
  implicit val bigDecimalEncoder: Encoder[BigDecimal] =
    encoder[BigDecimal] { bs => (idx, v) =>
      bs.setDecimal(idx, v.bigDecimal)
    }
  implicit val booleanEncoder = encoder(_.setBool)
  implicit val intEncoder = encoder(_.setInt)
  implicit val longEncoder = encoder(_.setLong)
  implicit val floatEncoder = encoder(_.setFloat)
  implicit val doubleEncoder = encoder(_.setDouble)
  implicit val byteArrayEncoder: Encoder[Array[Byte]] =
    encoder[Array[Byte]] { bs => (idx, v) =>
      bs.setBytes(idx, ByteBuffer.wrap(v))
    }
  implicit val uuidEncoder: Encoder[UUID] = encoder(_.setUUID)
  implicit val dateEncoder = encoder(_.setTimestamp)
}
