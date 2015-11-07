package io.getquill.source.sql

import io.getquill._
import io.getquill.source.sql.idiom.SqlIdiom
import scala.util.Try
import java.util.Date
import io.getquill.source.sql.naming.NamingStrategy

class SqlSourceMacroSpec extends Spec {

  "warns if the sql probing fails" in {
    case class Fail()
    "io.getquill.source.sql.mirror.mirrorSource.run(query[Fail])" mustNot compile
  }

  "fails if the query can't be translated to sql" in {
    val q = quote {
      qr1.flatMap(a => qr2.filter(b => b.s == a.s).take(1))
    }
    "io.getquill.source.sql.mirror.mirrorSource.run(q)" mustNot compile
  }

  "fails if the sql dialect is not valid" in {

    "testSource.run(qr1.delete)" mustNot compile

    class EvilDBDialect extends SqlIdiom {
      def prepare(sql: String) = sql
    }
    object testSource extends SqlSource[EvilDBDialect, NamingStrategy, Any, Any] {
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
