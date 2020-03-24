package io.getquill.dsl

import io.getquill.quotation.NonQuotedException
import scala.annotation.compileTimeOnly

private[dsl] trait InfixDsl {

  private[dsl] trait InfixValue {
    def as[T]: T
    def pure: InfixValue
  }

  implicit class InfixInterpolator(val sc: StringContext) {

    @compileTimeOnly(NonQuotedException.message)
    def infix(args: Any*): InfixValue = NonQuotedException()
  }
}
