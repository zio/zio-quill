package io.getquill.context.cassandra.encoding

import io.getquill.context.Context

import scala.language.experimental.macros

trait CollectionsEncoding {
  this: Context[_, _] =>

  implicit def listEncoder[T]: Encoder[List[T]] = macro ListEncodingMacro.listEncoder[T]
  implicit def listDecoder[T]: Decoder[List[T]] = macro ListEncodingMacro.listDecoder[T]

  implicit def setEncoder[T]: Encoder[Set[T]] = macro SetEncodingMacro.setEncoder[T]
  implicit def setDecoder[T]: Decoder[Set[T]] = macro SetEncodingMacro.setDecoder[T]

  implicit def mapEncoder[K, V]: Encoder[Map[K, V]] = macro MapEncodingMacro.mapEncoder[K, V]
  implicit def mapDecoder[K, V]: Decoder[Map[K, V]] = macro MapEncodingMacro.mapDecoder[K, V]
}
