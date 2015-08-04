package io

import language.implicitConversions
import language.experimental.macros
import io.getquill.impl.Quoted
import io.getquill.impl.Macro
import io.getquill.impl.Queryable
import io.getquill.impl.NonQuotedException

package object getquill {

  def table[T]: Queryable[T] = NonQuotedException()

  implicit def quote[T](body: T): Quoted[T] = macro Macro.quote[T]
  
  implicit def unquote[T](quoted: Quoted[T]): T = macro Macro.unquote[T]
}
