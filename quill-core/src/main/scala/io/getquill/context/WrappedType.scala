package io.getquill.context

trait WrappedValue[T] extends Any with WrappedType { self: AnyVal =>
  type Type = T
  def value: T
  override def toString() = s"$value"
}

trait WrappedType extends Any {
  type Type
  def value: Type
}