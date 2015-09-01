package io.getquill.source.sql.mirror

import java.util.Date
import io.getquill.source.mirror.Row
import scala.reflect.ClassTag

trait MirrorDecoders {
  this: mirrorSource.type =>

  private def decoder[T: ClassTag]: Decoder[T] = new Decoder[T] {
    def apply(index: Int, row: Row) =
      row[T](index)
  }

  implicit val stringDecoder = decoder[String]
  implicit val bigDecimalDecoder = decoder[BigDecimal]
  implicit val booleanDecoder = decoder[Boolean]
  implicit val byteDecoder = decoder[Byte]
  implicit val shortDecoder = decoder[Short]
  implicit val intDecoder = decoder[Int]
  implicit val longDecoder = decoder[Long]
  implicit val floatDecoder = decoder[Float]
  implicit val doubleDecoder = decoder[Double]
  implicit val byteArrayDecoder = decoder[Array[Byte]]
  implicit val dateDecoder = decoder[Date]
}
