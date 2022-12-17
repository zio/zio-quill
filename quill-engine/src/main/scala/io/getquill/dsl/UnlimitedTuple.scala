package io.getquill.dsl

import io.getquill.quotation.NonQuotedException
import scala.annotation.compileTimeOnly

object UnlimitedTuple {
  @compileTimeOnly(NonQuotedException.message)
  def apply(values: Any*): Nothing = NonQuotedException()
}