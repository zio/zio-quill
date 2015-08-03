package io

import language.implicitConversions
import language.experimental.macros
import io.getquill.impl.Quoted
import io.getquill.impl.Macro
import io.getquill.impl.Queryable
import io.getquill.impl.NonQuotedException
import io.getquill.impl.TableQueryable

package object getquill {

  def table[T]: TableQueryable[T] = NonQuotedException()

  def quote[T](body: T): Any = macro Macro.quote[T]
  
  implicit def unquote[T](quoted: Quoted[T]): T = macro Macro.unquote[T]
}
