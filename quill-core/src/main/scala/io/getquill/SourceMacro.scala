package io.getquill

import scala.reflect.macros.whitebox.Context
import io.getquill.ast.Query
import io.getquill.attach.TypeAttachment
import io.getquill.lifting.Lifting
import io.getquill.lifting.Unlifting

class SourceMacro(val c: Context) extends TypeAttachment with Lifting with Unlifting {
  import c.universe._

  def entity[T, R](implicit t: WeakTypeTag[T], r: WeakTypeTag[R]): Tree =
    to[Queryable[T]].attach(c.prefix.tree, ast.Table(t.tpe.typeSymbol.name.toString): Query)
}
