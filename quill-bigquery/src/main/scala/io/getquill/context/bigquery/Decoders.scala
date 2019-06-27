package io.getquill.context.bigquery

import com.google.cloud.bigquery.FieldValue
import io.getquill.BigQueryContext

trait Decoders {
  this: BigQueryContext[_] =>

  type Decoder[T] = BaseDecoder[T]

  def decoder[T](f: FieldValue => T): Decoder[T] = {
    (index: Index, row: ResultRow) =>
      {
        f(row.get(index))
      }
  }

  implicit def mappedDecoder[I, O](implicit mapped: MappedEncoding[I, O], d: Decoder[I]): Decoder[O] =
    mappedBaseDecoder(mapped, d)

  implicit val stringDecoder: Decoder[String] = decoder(_.getStringValue())
  implicit val bigDecimalDecoder: Decoder[BigDecimal] = decoder(_.getNumericValue())
  implicit val booleanDecoder: Decoder[Boolean] = decoder(_.getBooleanValue())
  implicit val intDecoder: Decoder[Int] = decoder(_.getLongValue().toInt)
  implicit val longDecoder: Decoder[Long] = decoder(_.getLongValue())
  implicit val byteDecoder: Decoder[Byte] = decoder(_.getLongValue().toByte)
  implicit val shortDecoder: Decoder[Short] = decoder(_.getLongValue().toShort)
  implicit val doubleDecoder: Decoder[Double] = decoder(_.getDoubleValue())
}