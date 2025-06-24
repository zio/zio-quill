package io.getquill.context.sql.encoding.mirror

import java.time.LocalDate
import java.util.Date

import io.getquill.SqlMirrorContext
import io.getquill.context.sql.encoding.ArrayEncoding

trait ArrayMirrorEncoding extends ArrayEncoding {
  this: SqlMirrorContext[_, _] =>

  implicit def arrayStringEncoder[Col <: collection.Seq[String]]: Encoder[Col]         = encoder[Col]
  implicit def arrayBigDecimalEncoder[Col <: collection.Seq[BigDecimal]]: Encoder[Col] = encoder[Col]
  implicit def arrayBooleanEncoder[Col <: collection.Seq[Boolean]]: Encoder[Col]       = encoder[Col]
  implicit def arrayByteEncoder[Col <: collection.Seq[Byte]]: Encoder[Col]             = encoder[Col]
  implicit def arrayShortEncoder[Col <: collection.Seq[Short]]: Encoder[Col]           = encoder[Col]
  implicit def arrayIntEncoder[Col <: collection.Seq[Int]]: Encoder[Col]               = encoder[Col]
  implicit def arrayLongEncoder[Col <: collection.Seq[Long]]: Encoder[Col]             = encoder[Col]
  implicit def arrayFloatEncoder[Col <: collection.Seq[Float]]: Encoder[Col]           = encoder[Col]
  implicit def arrayDoubleEncoder[Col <: collection.Seq[Double]]: Encoder[Col]         = encoder[Col]
  implicit def arrayDateEncoder[Col <: collection.Seq[Date]]: Encoder[Col]             = encoder[Col]
  implicit def arrayLocalDateEncoder[Col <: collection.Seq[LocalDate]]: Encoder[Col]   = encoder[Col]

  implicit def arrayStringDecoder[Col <: collection.Seq[String]](implicit bf: CBF[String, Col]): Decoder[Col] = decoderUnsafe[Col]
  implicit def arrayBigDecimalDecoder[Col <: collection.Seq[BigDecimal]](implicit bf: CBF[BigDecimal, Col]): Decoder[Col] =
    decoderUnsafe[Col]
  implicit def arrayBooleanDecoder[Col <: collection.Seq[Boolean]](implicit bf: CBF[Boolean, Col]): Decoder[Col] =
    decoderUnsafe[Col]
  implicit def arrayByteDecoder[Col <: collection.Seq[Byte]](implicit bf: CBF[Byte, Col]): Decoder[Col]       = decoderUnsafe[Col]
  implicit def arrayShortDecoder[Col <: collection.Seq[Short]](implicit bf: CBF[Short, Col]): Decoder[Col]    = decoderUnsafe[Col]
  implicit def arrayIntDecoder[Col <: collection.Seq[Int]](implicit bf: CBF[Int, Col]): Decoder[Col]          = decoderUnsafe[Col]
  implicit def arrayLongDecoder[Col <: collection.Seq[Long]](implicit bf: CBF[Long, Col]): Decoder[Col]       = decoderUnsafe[Col]
  implicit def arrayFloatDecoder[Col <: collection.Seq[Float]](implicit bf: CBF[Float, Col]): Decoder[Col]    = decoderUnsafe[Col]
  implicit def arrayDoubleDecoder[Col <: collection.Seq[Double]](implicit bf: CBF[Double, Col]): Decoder[Col] = decoderUnsafe[Col]
  implicit def arrayDateDecoder[Col <: collection.Seq[Date]](implicit bf: CBF[Date, Col]): Decoder[Col]       = decoderUnsafe[Col]
  implicit def arrayLocalDateDecoder[Col <: collection.Seq[LocalDate]](implicit bf: CBF[LocalDate, Col]): Decoder[Col] =
    decoderUnsafe[Col]
}
