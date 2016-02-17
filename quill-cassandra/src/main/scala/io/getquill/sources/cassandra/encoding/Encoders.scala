package io.getquill.sources.cassandra.encoding

import java.util.UUID
import com.datastax.driver.core.BoundStatement
import java.nio.ByteBuffer
import io.getquill.sources.cassandra.CassandraSource
import com.datastax.driver.core.Row
import io.getquill.sources.BindedStatementBuilder

trait Encoders {
  this: CassandraSource[_, Row, BindedStatementBuilder[BoundStatement]] =>

  def encoder[T](f: BoundStatement => (Int, T) => BoundStatement): Encoder[T] =
    new Encoder[T] {
      override def apply(idx: Int, value: T, row: BindedStatementBuilder[BoundStatement]) = {
        val raw = new io.getquill.sources.Encoder[BoundStatement, T] {
          override def apply(idx: Int, value: T, row: BoundStatement) =
            f(row)(idx, value)
        }
        row.single(idx, value, raw)
      }
    }

  implicit def setEncoder[T](implicit e: Encoder[T]): Encoder[Set[T]] =
    new Encoder[Set[T]] {
      override def apply(idx: Int, values: Set[T], row: BindedStatementBuilder[BoundStatement]) =
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
  implicit val dateEncoder = encoder(_.setDate)
}
