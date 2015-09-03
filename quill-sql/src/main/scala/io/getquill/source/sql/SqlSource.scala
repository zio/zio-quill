package io.getquill.source.sql

import language.experimental.macros
import java.util.Date
import scala.reflect.ClassTag
import io.getquill.quotation.Quoted
import io.getquill.source.sql.idiom.SqlIdiom
import scala.util.Try

abstract class SqlSource[D <: SqlIdiom, R: ClassTag, S: ClassTag] extends io.getquill.source.Source[R, S] {

  def run[T](quoted: Quoted[T]): Any = macro SqlSourceMacro.run[R, S, T]

  def probe(sql: String): Try[Any]

  implicit val stringDecoder: Decoder[String]
  implicit val bigDecimalDecoder: Decoder[BigDecimal]
  implicit val booleanDecoder: Decoder[Boolean]
  implicit val byteDecoder: Decoder[Byte]
  implicit val shortDecoder: Decoder[Short]
  implicit val intDecoder: Decoder[Int]
  implicit val longDecoder: Decoder[Long]
  implicit val floatDecoder: Decoder[Float]
  implicit val doubleDecoder: Decoder[Double]
  implicit val byteArrayDecoder: Decoder[Array[Byte]]
  implicit val dateDecoder: Decoder[Date]

  implicit val stringEncoder: Encoder[String]
  implicit val bigDecimalEncoder: Encoder[BigDecimal]
  implicit val booleanEncoder: Encoder[Boolean]
  implicit val byteEncoder: Encoder[Byte]
  implicit val shortEncoder: Encoder[Short]
  implicit val intEncoder: Encoder[Int]
  implicit val longEncoder: Encoder[Long]
  implicit val floatEncoder: Encoder[Float]
  implicit val doubleEncoder: Encoder[Double]
  implicit val byteArrayEncoder: Encoder[Array[Byte]]
  implicit val dateEncoder: Encoder[Date]
}
