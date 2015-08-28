package io.getquill.finagle.mysql

import io.getquill.util.Messages._
import com.twitter.finagle.exp.mysql._
import java.util.Date
import java.util.TimeZone

trait FinagleMysqlDecoders {
  this: FinagleMysqlSource =>

  protected val timestampValue =
    new TimestampValue(
      TimeZone.getDefault(),
      TimeZone.getTimeZone("UTC"))

  def decoder[T](f: PartialFunction[Value, T]) =
    new Decoder[T] {
      def apply(index: Int, row: Row) = {
        val value = row.values(index)
        f.lift(value).getOrElse(fail(s"Can't decode $value"))
      }
    }

  implicit val stringDecoder =
    decoder[String] {
      case StringValue(v) => v
    }
  implicit val bigDecimalDecoder =
    decoder[BigDecimal] {
      case BigDecimalValue(v) => v
    }
  implicit val booleanDecoder =
    decoder[Boolean] {
      case IntValue(v)   => v == 1
      case ShortValue(v) => v == 1
      case ByteValue(v)  => v == (1: Byte)
    }
  implicit val byteDecoder =
    decoder[Byte] {
      case ByteValue(v) => v
    }
  implicit val shortDecoder =
    decoder[Short] {
      case ShortValue(v) => v
      case IntValue(v)   => v.toShort
    }
  implicit val intDecoder =
    decoder[Int] {
      case IntValue(v)  => v
      case LongValue(v) => v.toInt
    }
  implicit val longDecoder =
    decoder[Long] {
      case LongValue(v) => v
      case IntValue(v)  => v
    }
  implicit val floatDecoder =
    decoder[Float] {
      case FloatValue(v) => v
    }
  implicit val doubleDecoder =
    decoder[Double] {
      case DoubleValue(v) => v
    }
  implicit val byteArrayDecoder =
    decoder[Array[Byte]] {
      case v: RawValue => v.bytes
    }
  implicit val dateDecoder =
    decoder[Date] {
      case `timestampValue`(v) => new Date(v.getTime)
    }
}
