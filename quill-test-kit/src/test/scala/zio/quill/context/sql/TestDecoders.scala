package zio.quill.context.sql

import zio.quill.MappedEncoding

trait TestDecoders {
  implicit val encodingTestTypeDecoder: MappedEncoding[String, EncodingTestType] =
    MappedEncoding[String, EncodingTestType](EncodingTestType)
  implicit val nameDecoder: MappedEncoding[String, Number] = MappedEncoding[String, Number](s =>
    Number
      .withValidation(s)
      .getOrElse(throw new Exception(s"Illegal number $s"))
  )
}
