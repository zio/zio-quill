package io.getquill.context.sql

trait TestDecoders {
  this: SqlContext[_, _] =>

  implicit val encodingTestTypeDecoder = MappedEncoding[String, EncodingTestType](EncodingTestType)
}
