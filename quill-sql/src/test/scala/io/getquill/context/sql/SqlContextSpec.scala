package io.getquill.context.sql

import java.util.Date

import scala.util.Try

import io.getquill.Spec
import io.getquill.context.mirror.Row
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.context.sql.testContext._
import io.getquill.Literal
import io.getquill.MirrorSqlDialect
import io.getquill.MirrorContextWithQueryProbing

class SqlContextSpec extends Spec {

  "binds inputs according to the sql terms order" - {
    "filter.update" in {
      val q = quote {
        qr1.filter(t => t.i == lift(1)).update(t => t.l -> lift(2L))
      }
      val mirror = testContext.run(q)
      mirror.string mustEqual "UPDATE TestEntity SET l = ? WHERE i = ?"
      mirror.prepareRow mustEqual Row(2l, 1)
    }
    "filter.map" in {
      val q = quote {
        qr1.filter(t => t.i == lift(1)).map(t => lift(2L))
      }
      val mirror = testContext.run(q)
      mirror.string mustEqual "SELECT ? FROM TestEntity t WHERE t.i = ?"
      mirror.prepareRow mustEqual Row(2l, 1)
    }
  }

  "fails if the sql probing fails" in {
    case class Fail()
    val s = new MirrorContextWithQueryProbing[MirrorSqlDialect, Literal]
    "s.run(query[Fail])" mustNot compile
  }

  "fails if the query can't be translated to sql" in {
    val ctx = new MirrorContextWithQueryProbing[MirrorSqlDialect, Literal]
    val q = quote {
      qr1.flatMap(a => qr2.filter(b => b.s == a.s).take(1))
    }
    "testContext.run(q)" mustNot compile
  }

  "fails if the sql dialect is not valid" in {

    "testContext.run(qr1.delete)" mustNot compile

    class EvilDBDialect extends SqlIdiom {
      override def liftingPlaceholder(index: Int): String = "?"
      override def prepareForProbing(string: String) = string
    }
    object testContext extends SqlContext[MirrorSqlDialect, Literal] {

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
