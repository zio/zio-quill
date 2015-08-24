package io

import scala.language.implicitConversions
import language.experimental.macros
import io.getquill.quotation.NonQuotedException
import io.getquill.quotation.Quoted
import io.getquill.EntityQueryable
import io.getquill.Macro

package object getquill {
  
  def queryable[T]: EntityQueryable[T] = NonQuotedException()

  def quote[T](body: T): Quoted[T] = macro Macro.quote[T]

  implicit def unquote[T](quoted: Quoted[T]): T = NonQuotedException()
}
