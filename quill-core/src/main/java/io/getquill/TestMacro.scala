package io.getquill

import scala.reflect.macros.whitebox.Context

class TestMacro(val c: Context) {
  import c.universe._

  def run[T](q: Expr[Queryable[T]])(implicit t: WeakTypeTag[T]) =
    q"${q.toString}"
}
