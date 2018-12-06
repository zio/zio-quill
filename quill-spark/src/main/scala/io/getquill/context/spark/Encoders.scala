package io.getquill.context.spark

import io.getquill.QuillSparkContext

trait Encoders {
  this: QuillSparkContext =>

  type Encoder[T] = BaseEncoder[T]
  type PrepareRow = List[Binding]

  def encoder[T](f: T => String): Encoder[T] =
    (index: Index, value: T, row: PrepareRow) =>
      row :+ ValueBinding(f(value))

  private def toStringEncoder[T]: Encoder[T] = encoder((v: T) => s"$v")

  implicit def mappedEncoder[I, O](implicit mapped: MappedEncoding[I, O], e: Encoder[O]): Encoder[I] =
    mappedBaseEncoder(mapped, e)

  implicit def optionEncoder[T](implicit d: Encoder[T]): Encoder[Option[T]] =
    (index: Index, value: Option[T], row: PrepareRow) =>
      value match {
        case None    => row :+ ValueBinding("null")
        case Some(v) => d(index, v, row)
      }

  implicit val stringEncoder: Encoder[String] = encoder(v => s"'${v.replaceAll("""[\\']""", """\\$0""")}'")
  implicit val bigDecimalEncoder: Encoder[BigDecimal] = toStringEncoder
  implicit val booleanEncoder: Encoder[Boolean] = toStringEncoder
  implicit val byteEncoder: Encoder[Byte] = toStringEncoder
  implicit val shortEncoder: Encoder[Short] = toStringEncoder
  implicit val intEncoder: Encoder[Int] = toStringEncoder
  implicit val longEncoder: Encoder[Long] = toStringEncoder
  implicit val doubleEncoder: Encoder[Double] = toStringEncoder
}