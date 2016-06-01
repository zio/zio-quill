package io.getquill.dsl

import io.getquill.quotation.NonQuotedException

private[dsl] trait InfixDsl {

  private[dsl] trait InfixValue {
    def as[T]: T
  }

  implicit class InfixInterpolator(val sc: StringContext) {
    def infix(args: Any*): InfixValue = NonQuotedException()
  }
}
