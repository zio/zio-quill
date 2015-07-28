package io

import language.implicitConversions
import language.experimental.macros
import io.getquill.meta.Meta
import io.getquill.meta.MetaMacro

package object getquill {

  def from[T]: Queryable[T] = ???

  def quote[T](body: T): Any = macro MetaMacro.quote[T]

  implicit def unquote[T](meta: Meta[T]): T = macro MetaMacro.unquote[T]
}
