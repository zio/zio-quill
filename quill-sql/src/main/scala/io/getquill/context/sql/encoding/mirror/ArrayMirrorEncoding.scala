package io.getquill.context.sql.encoding.mirror

import java.time.LocalDate
import java.util.Date

import io.getquill.SqlMirrorContext
import io.getquill.context.sql.encoding.ArrayEncoding

trait ArrayMirrorEncoding extends ArrayEncoding {
  this: SqlMirrorContext[_, _] =>

  implicit def arrayStringEncoder[Col <: Seq[String]]: Encoder[Col] = encoder[Col]
  implicit def arrayBigDecimalEncoder[Col <: Seq[BigDecimal]]: Encoder[Col] = encoder[Col]
  implicit def arrayBooleanEncoder[Col <: Seq[Boolean]]: Encoder[Col] = encoder[Col]
  implicit def arrayByteEncoder[Col <: Seq[Byte]]: Encoder[Col] = encoder[Col]
  implicit def arrayShortEncoder[Col <: Seq[Short]]: Encoder[Col] = encoder[Col]
  implicit def arrayIntEncoder[Col <: Seq[Int]]: Encoder[Col] = encoder[Col]
  implicit def arrayLongEncoder[Col <: Seq[Long]]: Encoder[Col] = encoder[Col]
  implicit def arrayFloatEncoder[Col <: Seq[Float]]: Encoder[Col] = encoder[Col]
  implicit def arrayDoubleEncoder[Col <: Seq[Double]]: Encoder[Col] = encoder[Col]
  implicit def arrayDateEncoder[Col <: Seq[Date]]: Encoder[Col] = encoder[Col]
  implicit def arrayLocalDateEncoder[Col <: Seq[LocalDate]]: Encoder[Col] = encoder[Col]

  implicit def arrayStringDecoder[Col <: Seq[String]](implicit bf: CBF[String, Col]): Decoder[Col] = decoderUnsafe[Col]
  implicit def arrayBigDecimalDecoder[Col <: Seq[BigDecimal]](implicit bf: CBF[BigDecimal, Col]): Decoder[Col] = decoderUnsafe[Col]
  implicit def arrayBooleanDecoder[Col <: Seq[Boolean]](implicit bf: CBF[Boolean, Col]): Decoder[Col] = decoderUnsafe[Col]
  implicit def arrayByteDecoder[Col <: Seq[Byte]](implicit bf: CBF[Byte, Col]): Decoder[Col] = decoderUnsafe[Col]
  implicit def arrayShortDecoder[Col <: Seq[Short]](implicit bf: CBF[Short, Col]): Decoder[Col] = decoderUnsafe[Col]
  implicit def arrayIntDecoder[Col <: Seq[Int]](implicit bf: CBF[Int, Col]): Decoder[Col] = decoderUnsafe[Col]
  implicit def arrayLongDecoder[Col <: Seq[Long]](implicit bf: CBF[Long, Col]): Decoder[Col] = decoderUnsafe[Col]
  implicit def arrayFloatDecoder[Col <: Seq[Float]](implicit bf: CBF[Float, Col]): Decoder[Col] = decoderUnsafe[Col]
  implicit def arrayDoubleDecoder[Col <: Seq[Double]](implicit bf: CBF[Double, Col]): Decoder[Col] = decoderUnsafe[Col]
  implicit def arrayDateDecoder[Col <: Seq[Date]](implicit bf: CBF[Date, Col]): Decoder[Col] = decoderUnsafe[Col]
  implicit def arrayLocalDateDecoder[Col <: Seq[LocalDate]](implicit bf: CBF[LocalDate, Col]): Decoder[Col] = decoderUnsafe[Col]
}

