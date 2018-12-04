package io.getquill.context.orientdb.encoding

import java.util.Date

import io.getquill.context.orientdb.OrientDBSessionContext
import io.getquill.util.Messages.fail

trait Decoders extends CollectionDecoders {
  this: OrientDBSessionContext[_] =>

  type Decoder[T] = OrientDBDecoder[T]

  case class OrientDBDecoder[T](decoder: BaseDecoder[T]) extends BaseDecoder[T] {
    override def apply(index: Index, row: ResultRow) =
      decoder(index, row)
  }

  def decoder[T](d: BaseDecoder[T]): Decoder[T] = OrientDBDecoder(
    (index, row) =>
      if (index >= row.fieldNames().length || row.fieldValues()(index) == null) {
        fail(s"Expected column at index $index to be defined but is was empty")
      } else
        d(index, row)
  )

  def decoder[T](f: ResultRow => Index => T): Decoder[T] =
    decoder((index, row) => f(row)(index))

  implicit def optionDecoder[T](implicit d: Decoder[T]): Decoder[Option[T]] =
    OrientDBDecoder((index, row) => {
      if (index < row.fieldValues().length) {
        row.fieldValues()(index) == null match {
          case true  => None
          case false => Some(d(index, row))
        }
      } else None
    })

  implicit def mappedDecoder[I, O](implicit mapped: MappedEncoding[I, O], decoder: Decoder[I]): Decoder[O] =
    OrientDBDecoder(mappedBaseDecoder(mapped, decoder.decoder))

  implicit val stringDecoder: Decoder[String] = decoder((index, row) => {
    row.field[String](row.fieldNames()(index))
  })
  implicit val doubleDecoder: Decoder[Double] = decoder((index, row) => row.field[Double](row.fieldNames()(index)))
  implicit val bigDecimalDecoder: Decoder[BigDecimal] = decoder((index, row) => row.field[java.math.BigDecimal](row.fieldNames()(index)))
  implicit val booleanDecoder: Decoder[Boolean] = decoder((index, row) => row.field[Boolean](row.fieldNames()(index)))
  implicit val intDecoder: Decoder[Int] = decoder((index, row) => row.field[Int](row.fieldNames()(index)))
  implicit val shortDecoder: Decoder[Short] = decoder((index, row) => row.field[Short](row.fieldNames()(index)))
  implicit val byteDecoder: Decoder[Byte] = decoder((index, row) => row.field[Byte](row.fieldNames()(index)))
  implicit val longDecoder: Decoder[Long] = decoder((index, row) => {
    if (row.fieldValues()(index).isInstanceOf[Int]) {
      row.field[Int](row.fieldNames()(index)).toLong
    } else
      row.field[Long](row.fieldNames()(index))
  })
  implicit val floatDecoder: Decoder[Float] = decoder((index, row) => row.field[Float](row.fieldNames()(index)))
  implicit val byteArrayDecoder: Decoder[Array[Byte]] = decoder((index, row) => {
    row.field[Array[Byte]](row.fieldNames()(index))
  })
  implicit val dateDecoder: Decoder[Date] = decoder((index, row) => row.field[Date](row.fieldNames()(index)))
}