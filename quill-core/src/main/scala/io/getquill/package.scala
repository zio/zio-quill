package io

import scala.language.implicitConversions
import language.experimental.macros
import io.getquill.quotation.NonQuotedException
import io.getquill.quotation.Quoted

package object getquill {

  def queryable[T]: EntityQueryable[T] = NonQuotedException()
  implicit def quote[T](body: T): Quoted[T] = macro Macro.quote[T]
  implicit def unquote[T](quoted: Quoted[T]): T = NonQuotedException()

  implicit class InfixInterpolator(val sc: StringContext) extends AnyVal {
    def infix(args: Any*) = InfixValue
  }

  object InfixValue {
    def as[T]: T = NonQuotedException()
  }
}
