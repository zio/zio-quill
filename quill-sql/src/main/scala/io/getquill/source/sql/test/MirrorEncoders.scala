package io.getquill.source.sql.test

import java.util.Date
import io.getquill.source.mirror.Row

trait MirrorEncoders {
  this: mirrorSource.type =>

  private def encoder[T] = new Encoder[T] {
    def apply(index: Int, value: T, row: Row) =
      row.add(value)
  }

  implicit val stringEncoder = encoder[String]
  implicit val bigDecimalEncoder = encoder[BigDecimal]
  implicit val booleanEncoder = encoder[Boolean]
  implicit val byteEncoder = encoder[Byte]
  implicit val shortEncoder = encoder[Short]
  implicit val intEncoder = encoder[Int]
  implicit val longEncoder = encoder[Long]
  implicit val floatEncoder = encoder[Float]
  implicit val doubleEncoder = encoder[Double]
  implicit val byteArrayEncoder = encoder[Array[Byte]]
  implicit val dateEncoder = encoder[Date]
}
