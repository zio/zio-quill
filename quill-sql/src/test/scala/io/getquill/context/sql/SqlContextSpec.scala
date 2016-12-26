package io.getquill.context.sql

import java.time.LocalDate
import java.util.{ Date, UUID }

import io.getquill._
import io.getquill.context.mirror.Row
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.context.sql.testContext._

import scala.util.Try

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

      override type PrepareRow = List[Any]
      override type ResultRow = List[Any]

      type Encoder[T] = BaseEncoder[T]
      type Decoder[T] = BaseDecoder[T]

      override def close = ()

      def probe(sql: String): Try[Any] = null

      def encoder[T]: Encoder[T] = (index: Index, value: T, row: PrepareRow) => row

      def decoder[T]: Decoder[T] = (index: Index, row: ResultRow) => row(index).asInstanceOf[T]

      implicit def optionEncoder[T](implicit d: Encoder[T]): Encoder[Option[T]] = encoder[Option[T]]

      implicit val stringEncoder: Encoder[String] = encoder[String]
      implicit val bigDecimalEncoder: Encoder[BigDecimal] = encoder[BigDecimal]
      implicit val booleanEncoder: Encoder[Boolean] = encoder[Boolean]
      implicit val byteEncoder: Encoder[Byte] = encoder[Byte]
      implicit val shortEncoder: Encoder[Short] = encoder[Short]
      implicit val intEncoder: Encoder[Int] = encoder[Int]
      implicit val longEncoder: Encoder[Long] = encoder[Long]
      implicit val floatEncoder: Encoder[Float] = encoder[Float]
      implicit val doubleEncoder: Encoder[Double] = encoder[Double]
      implicit val byteArrayEncoder: Encoder[Array[Byte]] = encoder[Array[Byte]]
      implicit val dateEncoder: Encoder[Date] = encoder[Date]
      implicit val localDateEncoder: Encoder[LocalDate] = encoder[LocalDate]
      implicit val uuidEncoder: Encoder[UUID] = encoder[UUID]

      implicit def optionDecoder[T](implicit d: Decoder[T]): Decoder[Option[T]] = decoder[Option[T]]

      implicit val stringDecoder: Decoder[String] = decoder[String]
      implicit val bigDecimalDecoder: Decoder[BigDecimal] = decoder[BigDecimal]
      implicit val booleanDecoder: Decoder[Boolean] = decoder[Boolean]
      implicit val byteDecoder: Decoder[Byte] = decoder[Byte]
      implicit val shortDecoder: Decoder[Short] = decoder[Short]
      implicit val intDecoder: Decoder[Int] = decoder[Int]
      implicit val longDecoder: Decoder[Long] = decoder[Long]
      implicit val floatDecoder: Decoder[Float] = decoder[Float]
      implicit val doubleDecoder: Decoder[Double] = decoder[Double]
      implicit val byteArrayDecoder: Decoder[Array[Byte]] = decoder[Array[Byte]]
      implicit val localDateDecoder: Decoder[LocalDate] = decoder[LocalDate]
      implicit val dateDecoder: Decoder[Date] = decoder[Date]
      implicit val uuidDecoder: Decoder[UUID] = decoder[UUID]

      implicit def mappedEncoder[I, O](implicit mapped: MappedEncoding[I, O], e: Encoder[O]): Encoder[I] =
        encoder[I]

      implicit def mappedDecoder[I, O](implicit mapped: MappedEncoding[I, O], d: Decoder[I]): Decoder[O] =
        decoder[O]
    }
  }
}
