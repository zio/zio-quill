package io.getquill.source.sql

import scala.reflect.ClassTag
import language.experimental.macros
import io.getquill.Actionable
import io.getquill.Queryable
import java.util.Date

abstract class SqlSource[R: ClassTag, S: ClassTag] extends io.getquill.source.Source[R, S] {

  def run[T](query: Queryable[T]): Any = macro SqlQueryMacro.run[R, S, T]
  def run[P1, T](query: P1 => Queryable[T])(p1: P1): Any = macro SqlQueryMacro.run1[P1, R, S, T]
  def run[P1, P2, T](query: (P1, P2) => Queryable[T])(p1: P1, p2: P2): Any = macro SqlQueryMacro.run2[P1, P2, R, S, T]

  def run[T](action: Actionable[T]): Any = macro SqlActionMacro.run[R, S, T]
  def run[P1, T](action: P1 => Actionable[T])(bindings: Iterable[P1]): Any = macro SqlActionMacro.run1[P1, R, S, T]
  def run[P1, P2, T](action: (P1, P2) => Actionable[T])(bindings: Iterable[(P1, P2)]): Any = macro SqlActionMacro.run2[P1, P2, R, S, T]

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
