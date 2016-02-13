package io

import scala.language.implicitConversions
import language.experimental.macros
import io.getquill.quotation.NonQuotedException
import io.getquill.sources._

package object getquill {

  def source[T <: Source[_, _]](config: SourceConfig[T]): T = macro Macro.quoteSource[T]
  
  def query[T]: EntityQuery[T] = NonQuotedException()

  def lift[T](v: T): T = v

  def quote[T](body: Quoted[T]): Quoted[T] = macro Macro.doubleQuote[T]
  implicit def quote[T](body: T): Quoted[T] = macro Macro.quote[T]
  implicit def unquote[T](quoted: Quoted[T]): T = NonQuotedException()

  implicit class InfixInterpolator(val sc: StringContext) extends AnyVal {
    def infix(args: Any*): InfixValue = NonQuotedException()
  }

  def mappedEncoding[I, O](f: I => O) = MappedEncoding(f)

  type Quoted[T] = quotation.Quoted[T]

  def Ord: OrdOps = NonQuotedException()

  implicit def orderingToOrd[T](implicit o: Ordering[T]): Ord[T] = NonQuotedException()
}
