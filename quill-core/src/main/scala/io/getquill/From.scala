package io.getquill

import language.experimental.macros

object From {
  
  def apply[T]: Queryable[T] = macro FromMacro.apply[T]
}