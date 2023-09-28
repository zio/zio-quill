package io.getquill.util

object NullCheck {
  def product(v: AnyRef): Boolean = v == null || v == None
}
