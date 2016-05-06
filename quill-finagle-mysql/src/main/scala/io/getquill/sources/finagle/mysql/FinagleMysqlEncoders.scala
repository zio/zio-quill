package io.getquill.sources.finagle.mysql

import java.util.Date
import com.twitter.finagle.exp.mysql.BigDecimalValue
import com.twitter.finagle.exp.mysql.CanBeParameter
import com.twitter.finagle.exp.mysql.CanBeParameter._
import com.twitter.finagle.exp.mysql.Parameter
import com.twitter.finagle.exp.mysql.Parameter.wrap
import io.getquill.sources.BindedStatementBuilder

trait FinagleMysqlEncoders {
  this: FinagleMysqlSource[_] =>

  def encoder[T](implicit cbp: CanBeParameter[T]): Encoder[T] =
    encoder[T]((v: T) => v: Parameter)

  def encoder[T](f: T => Parameter): Encoder[T] =
    new Encoder[T] {
      def apply(idx: Int, value: T, row: BindedStatementBuilder[List[Parameter]]) = {
        val raw = new io.getquill.sources.Encoder[List[Parameter], T] {
          override def apply(idx: Int, value: T, row: List[Parameter]) =
            row :+ f(value)
        }
        row.single[T](idx, value, raw)
      }
    }

  implicit def traversableEncoder[T](implicit e: Encoder[T]): Encoder[Traversable[T]] =
    new Encoder[Traversable[T]] {
      def apply(idx: Int, values: Traversable[T], row: BindedStatementBuilder[List[Parameter]]) =
        row.coll[T](idx, values, e)
    }

  private[this] val nullEncoder = encoder((_: Null) => Parameter.NullParameter)

  implicit def optionEncoder[T](implicit e: Encoder[T]): Encoder[Option[T]] =
    new Encoder[Option[T]] {
      def apply(idx: Int, value: Option[T], row: BindedStatementBuilder[List[Parameter]]) =
        value match {
          case None        => nullEncoder(idx, null, row)
          case Some(value) => e(idx, value, row)
        }
    }

  implicit val stringEncoder: Encoder[String] = encoder[String]
  implicit val bigDecimalEncoder: Encoder[BigDecimal] =
    encoder[BigDecimal] { (value: BigDecimal) =>
      BigDecimalValue(value): Parameter
    }
  implicit val booleanEncoder: Encoder[Boolean] = encoder[Boolean]
  implicit val byteEncoder: Encoder[Byte] = encoder[Byte]
  implicit val shortEncoder: Encoder[Short] = encoder[Short]
  implicit val intEncoder: Encoder[Int] = encoder[Int]
  implicit val longEncoder: Encoder[Long] = encoder[Long]
  implicit val floatEncoder: Encoder[Float] = encoder[Float]
  implicit val doubleEncoder: Encoder[Double] = encoder[Double]
  implicit val byteArrayEncoder: Encoder[Array[Byte]] = encoder[Array[Byte]]
  implicit val dateEncoder: Encoder[Date] = encoder[Date]
}
