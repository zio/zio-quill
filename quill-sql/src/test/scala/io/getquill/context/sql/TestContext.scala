package io.getquill.context.sql

import java.time.LocalDate
import java.util.Date

import io.getquill.{ MirrorSqlDialect, NamingStrategy, SqlMirrorContext, TestEntities, Literal }
import io.getquill.context.sql.encoding.ArrayEncoding

class TestContextTemplate[Naming <: NamingStrategy]
  extends SqlMirrorContext[MirrorSqlDialect, Naming]
  with TestEntities
  with TestEncoders
  with TestDecoders {

  def withNaming[N <: NamingStrategy](f: TestContextTemplate[N] => Any): Unit = {
    val ctx = new TestContextTemplate[N]
    f(ctx)
    ctx.close
  }
}

object testContext extends TestContextTemplate[Literal]

object testContextWithArrays extends TestContextTemplate[Literal] with ArrayEncoding {
  implicit def arrayStringEncoder[Col <: Seq[String]]: Encoder[Col] = encoder[Col]
  implicit def arrayBigDecimalEncoder[Col <: Seq[BigDecimal]]: Encoder[Col] = encoder[Col]
  implicit def arrayBooleanEncoder[Col <: Seq[Boolean]]: Encoder[Col] = encoder[Col]
  implicit def arrayByteEncoder[Col <: Seq[Byte]]: Encoder[Col] = encoder[Col]
  implicit def arrayShortEncoder[Col <: Seq[Short]]: Encoder[Col] = encoder[Col]
  implicit def arrayIntEncoder[Col <: Seq[Index]]: Encoder[Col] = encoder[Col]
  implicit def arrayLongEncoder[Col <: Seq[Long]]: Encoder[Col] = encoder[Col]
  implicit def arrayFloatEncoder[Col <: Seq[Float]]: Encoder[Col] = encoder[Col]
  implicit def arrayDoubleEncoder[Col <: Seq[Double]]: Encoder[Col] = encoder[Col]
  implicit def arrayDateEncoder[Col <: Seq[Date]]: Encoder[Col] = encoder[Col]
  implicit def arrayLocalDateEncoder[Col <: Seq[LocalDate]]: Encoder[Col] = encoder[Col]
  implicit def arrayStringDecoder[Col <: Seq[String]](implicit bf: testContextWithArrays.CBF[String, Col]): Decoder[Col] = decoderUnsafe[Col]
  implicit def arrayBigDecimalDecoder[Col <: Seq[BigDecimal]](implicit bf: testContextWithArrays.CBF[BigDecimal, Col]): Decoder[Col] = decoderUnsafe[Col]
  implicit def arrayBooleanDecoder[Col <: Seq[Boolean]](implicit bf: testContextWithArrays.CBF[Boolean, Col]): Decoder[Col] = decoderUnsafe[Col]
  implicit def arrayByteDecoder[Col <: Seq[Byte]](implicit bf: testContextWithArrays.CBF[Byte, Col]): Decoder[Col] = decoderUnsafe[Col]
  implicit def arrayShortDecoder[Col <: Seq[Short]](implicit bf: testContextWithArrays.CBF[Short, Col]): Decoder[Col] = decoderUnsafe[Col]
  implicit def arrayIntDecoder[Col <: Seq[Index]](implicit bf: testContextWithArrays.CBF[Index, Col]): Decoder[Col] = decoderUnsafe[Col]
  implicit def arrayLongDecoder[Col <: Seq[Long]](implicit bf: testContextWithArrays.CBF[Long, Col]): Decoder[Col] = decoderUnsafe[Col]
  implicit def arrayFloatDecoder[Col <: Seq[Float]](implicit bf: testContextWithArrays.CBF[Float, Col]): Decoder[Col] = decoderUnsafe[Col]
  implicit def arrayDoubleDecoder[Col <: Seq[Double]](implicit bf: testContextWithArrays.CBF[Double, Col]): Decoder[Col] = decoderUnsafe[Col]
  implicit def arrayDateDecoder[Col <: Seq[Date]](implicit bf: testContextWithArrays.CBF[Date, Col]): Decoder[Col] = decoderUnsafe[Col]
  implicit def arrayLocalDateDecoder[Col <: Seq[LocalDate]](implicit bf: testContextWithArrays.CBF[LocalDate, Col]): Decoder[Col] = decoderUnsafe[Col]
}