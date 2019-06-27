package io.getquill.context.bigquery

import com.google.cloud.bigquery.QueryParameterValue
import io.getquill.BigQueryContext

trait Encoders {
  this: BigQueryContext[_] =>

  type Encoder[T] = BaseEncoder[T]

  def encoder[T](f: T => QueryParameterValue): Encoder[T] = {
    (index: Index, value: T, row: PrepareRow) =>
      {
        row.addPositionalParameter(f(value))
        row
      }
  }

  implicit def mappedEncoder[I, O](implicit mapped: MappedEncoding[I, O], e: Encoder[O]): Encoder[I] =
    mappedBaseEncoder(mapped, e)

  implicit val stringEncoder: Encoder[String] = encoder(QueryParameterValue.string)
  implicit val bigDecimalEncoder: Encoder[BigDecimal] = encoder[BigDecimal](b => QueryParameterValue.numeric(b.bigDecimal))
  implicit val booleanEncoder: Encoder[Boolean] = encoder[Boolean](b => QueryParameterValue.bool(Boolean.box(b)))
  implicit val intEncoder: Encoder[Int] = encoder[Int](i => QueryParameterValue.int64(Int.box(i)))
  implicit val longEncoder: Encoder[Long] = encoder[Long](l => QueryParameterValue.int64(Long.box(l)))
  implicit val byteEncoder: Encoder[Byte] = encoder[Byte](b => QueryParameterValue.int64(Int.box(b.toInt)))
  implicit val shortEncoder: Encoder[Short] = encoder[Short](s => QueryParameterValue.int64(Int.box(s.toInt)))
  implicit val doubleEncoder: Encoder[Double] = encoder[Double](d => QueryParameterValue.float64(Double.box(d)))

}