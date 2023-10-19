package io.getquill.util

object NullCheck {
  def product(v: AnyRef) = v == null || v == None
}
