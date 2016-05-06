package io.getquill.sources.cassandra.encoding

import java.util.UUID
import scala.math.BigDecimal.javaBigDecimal2bigDecimal
import com.datastax.driver.core.Row
import io.getquill.sources.cassandra.CassandraSource
import com.datastax.driver.core.BoundStatement
import io.getquill.sources.BindedStatementBuilder

trait Decoders {
  this: CassandraSource[_, Row, BindedStatementBuilder[BoundStatement]] =>

  private def decoder[T](f: Row => Int => T): Decoder[T] =
    (index: Int, row: Row) => f(row)(index)

  implicit def optionDecoder[T](implicit d: Decoder[T]): Decoder[Option[T]] =
    (index: Int, row: Row) => {
      row.isNull(index) match {
        case true => None
        case false => Some(d(index, row))
      }
    }

  implicit val stringDecoder = decoder(_.getString)
  implicit val bigDecimalDecoder: Decoder[BigDecimal] = (index: Int, row: Row) => row.getDecimal(index)
  implicit val booleanDecoder = decoder(_.getBool)
  implicit val intDecoder = decoder(_.getInt)
  implicit val longDecoder = decoder(_.getLong)
  implicit val floatDecoder = decoder(_.getFloat)
  implicit val doubleDecoder = decoder(_.getDouble)
  implicit val byteArrayDecoder: Decoder[Array[Byte]] =
    (index: Int, row: Row) => {
      val bb = row.getBytes(index)
      val b = new Array[Byte](bb.remaining())
      bb.get(b)
      b
    }
  implicit val uuidDecoder: Decoder[UUID] =
    (index: Int, row: Row) => {
      row.getUUID(index)
    }
  implicit val dateDecoder = decoder(_.getTimestamp)
}
