package io.getquill.sources.sql

import language.higherKinds
import io.getquill.Query
import io.getquill.Action
import java.util.Date
import language.experimental.macros
import scala.reflect.ClassTag
import scala.util.Try
import io.getquill.naming.NamingStrategy
import io.getquill.quotation.Quoted
import io.getquill.sources.sql.idiom.SqlIdiom

abstract class SqlSource[D <: SqlIdiom, N <: NamingStrategy, R: ClassTag, S: ClassTag] extends io.getquill.sources.Source[R, S] {

  type QueryResult[T]
  type ActionResult[T]
  type BatchedActionResult[T]

  def probe(sql: String): Try[Any]

  implicit def optionDecoder[T](implicit d: Decoder[T]): Decoder[Option[T]]
  implicit def optionEncoder[T](implicit d: Encoder[T]): Encoder[Option[T]]

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

  def run[T](
    quoted: Quoted[Query[T]]): QueryResult[T] = macro SqlSourceMacro.run[R, S]
  def run[P1, T](
    quoted: Quoted[P1 => Query[T]]): P1 => QueryResult[T] = macro SqlSourceMacro.run[R, S]
  def run[P1, P2, T](
    quoted: Quoted[(P1, P2) => Query[T]]): (P1, P2) => QueryResult[T] = macro SqlSourceMacro.run[R, S]
  def run[P1, P2, P3, T](
    quoted: Quoted[(P1, P2, P3) => Query[T]]): (P1, P2, P3) => QueryResult[T] = macro SqlSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, T](
    quoted: Quoted[(P1, P2, P3, P4) => Query[T]]): (P1, P2, P3, P4) => QueryResult[T] = macro SqlSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, T](
    quoted: Quoted[(P1, P2, P3, P4, P5) => Query[T]]): (P1, P2, P3, P4, P5) => QueryResult[T] = macro SqlSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6) => Query[T]]): (P1, P2, P3, P4, P5, P6) => QueryResult[T] = macro SqlSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7) => Query[T]]): (P1, P2, P3, P4, P5, P6, P7) => QueryResult[T] = macro SqlSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8) => Query[T]]): (P1, P2, P3, P4, P5, P6, P7, P8) => QueryResult[T] = macro SqlSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9) => Query[T]]): (P1, P2, P3, P4, P5, P6, P7, P8, P9) => QueryResult[T] = macro SqlSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) => Query[T]]): (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) => QueryResult[T] = macro SqlSourceMacro.run[R, S]
  
  def run[T](
    quoted: Quoted[Action[T]]): ActionResult[T] = macro SqlSourceMacro.run[R, S]
  def run[P1, T](
    quoted: Quoted[P1 => Action[T]]): List[P1] => BatchedActionResult[T] = macro SqlSourceMacro.run[R, S]
  def run[P1, P2, T](
    quoted: Quoted[(P1, P2) => Action[T]]): List[(P1, P2)] => BatchedActionResult[T] = macro SqlSourceMacro.run[R, S]
  def run[P1, P2, P3, T](
    quoted: Quoted[(P1, P2, P3) => Action[T]]): List[(P1, P2, P3)] => BatchedActionResult[T] = macro SqlSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, T](
    quoted: Quoted[(P1, P2, P3, P4) => Action[T]]): List[(P1, P2, P3, P4)] => BatchedActionResult[T] = macro SqlSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, T](
    quoted: Quoted[(P1, P2, P3, P4, P5) => Action[T]]): List[(P1, P2, P3, P4, P5)] => BatchedActionResult[T] = macro SqlSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6) => Action[T]]): List[(P1, P2, P3, P4, P5, P6)] => BatchedActionResult[T] = macro SqlSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7) => Action[T]]): List[(P1, P2, P3, P4, P5, P6, P7)] => BatchedActionResult[T] = macro SqlSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8) => Action[T]]): List[(P1, P2, P3, P4, P5, P6, P7, P8)] => BatchedActionResult[T] = macro SqlSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9) => Action[T]]): List[(P1, P2, P3, P4, P5, P6, P7, P8, P9)] => BatchedActionResult[T] = macro SqlSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) => Action[T]]): List[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10)] => BatchedActionResult[T] = macro SqlSourceMacro.run[R, S]
  
  def run[T](
    quoted: Quoted[T]): QueryResult[T] = macro SqlSourceMacro.run[R, S]
  def run[P1, T](
    quoted: Quoted[P1 => T]): P1 => QueryResult[T] = macro SqlSourceMacro.run[R, S]
  def run[P1, P2, T](
    quoted: Quoted[(P1, P2) => T]): (P1, P2) => QueryResult[T] = macro SqlSourceMacro.run[R, S]
  def run[P1, P2, P3, T](
    quoted: Quoted[(P1, P2, P3) => T]): (P1, P2, P3) => QueryResult[T] = macro SqlSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, T](
    quoted: Quoted[(P1, P2, P3, P4) => T]): (P1, P2, P3, P4) => QueryResult[T] = macro SqlSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, T](
    quoted: Quoted[(P1, P2, P3, P4, P5) => T]): (P1, P2, P3, P4, P5) => QueryResult[T] = macro SqlSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6) => T]): (P1, P2, P3, P4, P5, P6) => QueryResult[T] = macro SqlSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7) => T]): (P1, P2, P3, P4, P5, P6, P7) => QueryResult[T] = macro SqlSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8) => T]): (P1, P2, P3, P4, P5, P6, P7, P8) => QueryResult[T] = macro SqlSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9) => T]): (P1, P2, P3, P4, P5, P6, P7, P8, P9) => QueryResult[T] = macro SqlSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) => T]): (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) => QueryResult[T] = macro SqlSourceMacro.run[R, S]
}
