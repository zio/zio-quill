package io.getquill.context.cassandra.encoding

import java.util.UUID
import scala.math.BigDecimal.javaBigDecimal2bigDecimal
import com.datastax.driver.core.Row
import io.getquill.context.cassandra.CassandraContext
import com.datastax.driver.core.BoundStatement
import io.getquill.context.BindedStatementBuilder
import io.getquill.util.Messages.fail

trait Decoders {
  this: CassandraContext[_, Row, BindedStatementBuilder[BoundStatement]] =>

  def decoder[T](f: Row => Int => T): Decoder[T] =
    new Decoder[T] {
      def apply(index: Int, row: Row) = {
        row.isNull(index) match {
          case true  => fail(s"Expected column at index $index to be defined but is was empty")
          case false => f(row)(index)
        }
      }
    }

  implicit def optionDecoder[T](implicit d: Decoder[T]): Decoder[Option[T]] =
    new Decoder[Option[T]] {
      def apply(index: Int, row: Row) = {
        row.isNull(index) match {
          case true  => None
          case false => Some(d(index, row))
        }
      }
    }

  implicit val stringDecoder = decoder(_.getString)
  implicit val bigDecimalDecoder: Decoder[BigDecimal] =
    new Decoder[BigDecimal] {
      def apply(index: Int, row: Row) =
        row.getDecimal(index)
    }
  implicit val booleanDecoder = decoder(_.getBool)
  implicit val intDecoder = decoder(_.getInt)
  implicit val longDecoder = decoder(_.getLong)
  implicit val floatDecoder = decoder(_.getFloat)
  implicit val doubleDecoder = decoder(_.getDouble)
  implicit val byteArrayDecoder: Decoder[Array[Byte]] =
    new Decoder[Array[Byte]] {
      def apply(index: Int, row: Row) = {
        val bb = row.getBytes(index)
        val b = new Array[Byte](bb.remaining())
        bb.get(b)
        b
      }
    }
  implicit val uuidDecoder: Decoder[UUID] =
    new Decoder[UUID] {
      def apply(index: Int, row: Row) = {
        row.getUUID(index)
      }
    }
  implicit val dateDecoder = decoder(_.getTimestamp)
}
