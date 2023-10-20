package io.getquill.dsl

import io.getquill.quotation.NonQuotedException

import scala.annotation.compileTimeOnly

private[getquill] trait InfixDsl {

  private[getquill] trait InfixValue {
    def as[T]: T
    def asCondition: Boolean
    def pure: InfixValue
    private[getquill] def generic: InfixValue
    private[getquill] def transparent: InfixValue
  }

  implicit final class InfixInterpolator(val sc: StringContext) {

    @compileTimeOnly(NonQuotedException.message)
    @deprecated("""Use sql"${content}" instead""", "3.3.0")
    def infix(args: Any*): InfixValue = NonQuotedException()
  }

  implicit final class SqlInfixInterpolator(val sc: StringContext) {

    @compileTimeOnly(NonQuotedException.message)
    def sql(args: Any*): InfixValue = NonQuotedException()
  }

  object compat {
    // For compatibility with Slick/Doobie/etc... that already have an SQL interpolator
    implicit final class QsqlInfixInterpolator(val sc: StringContext) {

      @compileTimeOnly(NonQuotedException.message)
      def qsql(args: Any*): InfixValue = NonQuotedException()
    }
  }
}
