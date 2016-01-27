package io.getquill.sources.sql

import io.getquill._
import io.getquill.naming.NamingStrategy
import io.getquill.sources.sql.idiom.SqlIdiom
import scala.util.Try
import java.util.Date
import io.getquill.sources.mirror.Row

class SqlSourceMacroSpec extends Spec {

  "binds inputs according to the sql terms order" - {
    "filter.update" in {
      val q = quote {
        (i: Int, l: Long) =>
          qr1.filter(t => t.i == i).update(t => t.l -> l)
      }
      val mirror = mirrorSource.run(q)(List((1, 2L)))
      mirror.sql mustEqual "UPDATE TestEntity SET l = ? WHERE i = ?"
      mirror.bindList mustEqual List(Row(2l, 1))
    }
    "filter.map" in {
      val q = quote {
        (i: Int, l: Long) =>
          qr1.filter(t => t.i == i).map(t => l)
      }
      val mirror = mirrorSource.run(q)(1, 2L)
      mirror.sql mustEqual "SELECT ? FROM TestEntity t WHERE t.i = ?"
      mirror.binds mustEqual Row(2l, 1)
    }
  }

  "warns if the sql probing fails" in {
    case class Fail()
    "mirrorSource.run(query[Fail])" mustNot compile
  }

  "fails if the query can't be translated to sql" in {
    val q = quote {
      qr1.flatMap(a => qr2.filter(b => b.s == a.s).take(1))
    }
    "io.getquill.sources.sql.mirror.mirrorSource.run(q)" mustNot compile
  }

  "fails if the sql dialect is not valid" in {

    "testSource.run(qr1.delete)" mustNot compile

    class EvilDBDialect extends SqlIdiom {
      def prepare(sql: String) = sql
    }
    object testSource extends SqlSource[EvilDBDialect, NamingStrategy, Any, Any] {

      override def close = ()
      def probe(sql: String): Try[Any] = null

      implicit def optionDecoder[T](implicit d: Decoder[T]): Decoder[Option[T]] = null
      implicit def optionEncoder[T](implicit d: Encoder[T]): Encoder[Option[T]] = null

      implicit val stringDecoder: Decoder[String] = null
      implicit val bigDecimalDecoder: Decoder[BigDecimal] = null
      implicit val booleanDecoder: Decoder[Boolean] = null
      implicit val byteDecoder: Decoder[Byte] = null
      implicit val shortDecoder: Decoder[Short] = null
      implicit val intDecoder: Decoder[Int] = null
      implicit val longDecoder: Decoder[Long] = null
      implicit val floatDecoder: Decoder[Float] = null
      implicit val doubleDecoder: Decoder[Double] = null
      implicit val byteArrayDecoder: Decoder[Array[Byte]] = null
      implicit val dateDecoder: Decoder[Date] = null

      implicit val stringEncoder: Encoder[String] = null
      implicit val bigDecimalEncoder: Encoder[BigDecimal] = null
      implicit val booleanEncoder: Encoder[Boolean] = null
      implicit val byteEncoder: Encoder[Byte] = null
      implicit val shortEncoder: Encoder[Short] = null
      implicit val intEncoder: Encoder[Int] = null
      implicit val longEncoder: Encoder[Long] = null
      implicit val floatEncoder: Encoder[Float] = null
      implicit val doubleEncoder: Encoder[Double] = null
      implicit val byteArrayEncoder: Encoder[Array[Byte]] = null
      implicit val dateEncoder: Encoder[Date] = null
    }
  }
}
