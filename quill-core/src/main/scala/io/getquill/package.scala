package io

import scala.language.implicitConversions
import language.experimental.macros
import io.getquill.quotation.NonQuotedException
import io.getquill.quotation.Quoted

package object getquill {

  def query[T]: EntityQuery[T] = NonQuotedException()
  implicit def quote[T](body: T): Quoted[T] = macro Macro.quote[T]
  implicit def unquote[T](quoted: Quoted[T]): T = NonQuotedException()

  implicit class InfixInterpolator(val sc: StringContext) extends AnyVal {
    def infix(args: Any*): InfixValue = NonQuotedException()
  }

  def mappedEncoding[I, O](f: I => O) = source.MappedEncoding(f)
}
