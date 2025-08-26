package io.getquill.context.sql.encoding.mirror

import java.time.LocalDate
import java.util.Date

import io.getquill.SqlMirrorContext
import io.getquill.context.sql.encoding.ArrayEncoding

trait ArrayMirrorEncoding extends ArrayEncoding {
  this: SqlMirrorContext[_, _] =>

  implicit def arrayStringEncoder[Col <: Iterable[String]]: Encoder[Col]         = encoder[Col]
  implicit def arrayBigDecimalEncoder[Col <: Iterable[BigDecimal]]: Encoder[Col] = encoder[Col]
  implicit def arrayBooleanEncoder[Col <: Iterable[Boolean]]: Encoder[Col]       = encoder[Col]
  implicit def arrayByteEncoder[Col <: Iterable[Byte]]: Encoder[Col]             = encoder[Col]
  implicit def arrayShortEncoder[Col <: Iterable[Short]]: Encoder[Col]           = encoder[Col]
  implicit def arrayIntEncoder[Col <: Iterable[Int]]: Encoder[Col]               = encoder[Col]
  implicit def arrayLongEncoder[Col <: Iterable[Long]]: Encoder[Col]             = encoder[Col]
  implicit def arrayFloatEncoder[Col <: Iterable[Float]]: Encoder[Col]           = encoder[Col]
  implicit def arrayDoubleEncoder[Col <: Iterable[Double]]: Encoder[Col]         = encoder[Col]
  implicit def arrayDateEncoder[Col <: Iterable[Date]]: Encoder[Col]             = encoder[Col]
  implicit def arrayLocalDateEncoder[Col <: Iterable[LocalDate]]: Encoder[Col]   = encoder[Col]

  implicit def arrayStringDecoder[Col <: Iterable[String]](implicit bf: CBF[String, Col]): Decoder[Col] = decoderUnsafe[Col]
  implicit def arrayBigDecimalDecoder[Col <: Iterable[BigDecimal]](implicit bf: CBF[BigDecimal, Col]): Decoder[Col] =
    decoderUnsafe[Col]
  implicit def arrayBooleanDecoder[Col <: Iterable[Boolean]](implicit bf: CBF[Boolean, Col]): Decoder[Col] =
    decoderUnsafe[Col]
  implicit def arrayByteDecoder[Col <: Iterable[Byte]](implicit bf: CBF[Byte, Col]): Decoder[Col]       = decoderUnsafe[Col]
  implicit def arrayShortDecoder[Col <: Iterable[Short]](implicit bf: CBF[Short, Col]): Decoder[Col]    = decoderUnsafe[Col]
  implicit def arrayIntDecoder[Col <: Iterable[Int]](implicit bf: CBF[Int, Col]): Decoder[Col]          = decoderUnsafe[Col]
  implicit def arrayLongDecoder[Col <: Iterable[Long]](implicit bf: CBF[Long, Col]): Decoder[Col]       = decoderUnsafe[Col]
  implicit def arrayFloatDecoder[Col <: Iterable[Float]](implicit bf: CBF[Float, Col]): Decoder[Col]    = decoderUnsafe[Col]
  implicit def arrayDoubleDecoder[Col <: Iterable[Double]](implicit bf: CBF[Double, Col]): Decoder[Col] = decoderUnsafe[Col]
  implicit def arrayDateDecoder[Col <: Iterable[Date]](implicit bf: CBF[Date, Col]): Decoder[Col]       = decoderUnsafe[Col]
  implicit def arrayLocalDateDecoder[Col <: Iterable[LocalDate]](implicit bf: CBF[LocalDate, Col]): Decoder[Col] =
    decoderUnsafe[Col]
}
