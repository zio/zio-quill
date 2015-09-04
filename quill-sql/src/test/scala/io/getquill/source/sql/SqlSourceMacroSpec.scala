package io.getquill.source.sql

import io.getquill._
import io.getquill.source.sql.idiom.SqlIdiom
import scala.util.Try
import java.util.Date
import io.getquill.source.sql.mirror.mirrorSource

class SqlSourceMacroSpec extends Spec {

  "warns if the sql probing fails" in {
    case class Fail()
    "mirrorSource.run(queryable[Fail])" must compile
  }

  "fails if the sql dialect is not valid" in {

    "testSource.run(qr1.delete)" mustNot compile

    class EvilDBDialect extends SqlIdiom
    object testSource extends SqlSource[EvilDBDialect, Any, Any] {
      def probe(sql: String): Try[Any] = ???

      implicit val stringDecoder: Decoder[String] = ???
      implicit val bigDecimalDecoder: Decoder[BigDecimal] = ???
      implicit val booleanDecoder: Decoder[Boolean] = ???
      implicit val byteDecoder: Decoder[Byte] = ???
      implicit val shortDecoder: Decoder[Short] = ???
      implicit val intDecoder: Decoder[Int] = ???
      implicit val longDecoder: Decoder[Long] = ???
      implicit val floatDecoder: Decoder[Float] = ???
      implicit val doubleDecoder: Decoder[Double] = ???
      implicit val byteArrayDecoder: Decoder[Array[Byte]] = ???
      implicit val dateDecoder: Decoder[Date] = ???

      implicit val stringEncoder: Encoder[String] = ???
      implicit val bigDecimalEncoder: Encoder[BigDecimal] = ???
      implicit val booleanEncoder: Encoder[Boolean] = ???
      implicit val byteEncoder: Encoder[Byte] = ???
      implicit val shortEncoder: Encoder[Short] = ???
      implicit val intEncoder: Encoder[Int] = ???
      implicit val longEncoder: Encoder[Long] = ???
      implicit val floatEncoder: Encoder[Float] = ???
      implicit val doubleEncoder: Encoder[Double] = ???
      implicit val byteArrayEncoder: Encoder[Array[Byte]] = ???
      implicit val dateEncoder: Encoder[Date] = ???
    }
  }
}
