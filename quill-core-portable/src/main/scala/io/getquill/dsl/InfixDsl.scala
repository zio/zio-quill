package io.getquill.dsl

import io.getquill.quotation.NonQuotedException

import scala.annotation.compileTimeOnly

private[getquill] trait InfixDsl {

  private[getquill] trait InfixValue {
    def as[T]: T
    def asCondition: Boolean
    def pure: InfixValue
    private[getquill] def generic: InfixValue
  }

  implicit class InfixInterpolator(val sc: StringContext) {

    @compileTimeOnly(NonQuotedException.message)
    def infix(args: Any*): InfixValue = NonQuotedException()
  }
}
