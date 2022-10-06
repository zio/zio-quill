package io.getquill

trait JsonValueBase[T] { def value: T }
case class JsonValue[T](value: T) extends JsonValueBase[T]
case class JsonbValue[T](value: T) extends JsonValueBase[T]
