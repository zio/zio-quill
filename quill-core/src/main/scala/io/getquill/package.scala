package io

import language.implicitConversions
import language.experimental.macros

package object getquill {

  def from[T]: Queryable[T] = ???

  def quote[T](body: T): Any = macro Quotation.quote[T]

  implicit def unquote[T](quoted: Quoted[T]): T = macro Quotation.unquote[T]
}
