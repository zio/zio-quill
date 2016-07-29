package io.getquill.context.sql

trait TestDecoders {
  this: SqlContext[_, _] =>

  implicit val encodingTestTypeDecoder = mappedEncoding[String, EncodingTestType](EncodingTestType)
}
