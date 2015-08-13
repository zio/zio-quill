package io

import scala.language.implicitConversions
import language.experimental.macros
import io.getquill.impl.NonQuotedException
import io.getquill.impl.Quoted
import io.getquill.impl.EntityQueryable
import io.getquill.impl.Macro

package object getquill {
  
  def queryable[T]: EntityQueryable[T] = NonQuotedException()

  implicit def quote[T](body: T): Quoted[T] = macro Macro.quote[T]

  implicit def unquote[T](quoted: Quoted[T]): T = macro Macro.unquote[T]
}
