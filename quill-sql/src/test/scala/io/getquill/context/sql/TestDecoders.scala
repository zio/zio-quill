package io.getquill.context.sql

import io.getquill.MappedEncoding

trait TestDecoders {
  implicit val encodingTestTypeDecoder = MappedEncoding[String, EncodingTestType](EncodingTestType)
  implicit val nameDecoder = MappedEncoding[String, Number](s => Number.withValidation(s)
    .getOrElse(throw new Exception(s"Illegal number $s")))
}
