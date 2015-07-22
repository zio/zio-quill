package io.getquill

import scala.reflect.macros.whitebox.Context
import io.getquill.attach.TypeAttachment
import lifting.Lifting

class FromMacro(val c: Context) extends TypeAttachment with Lifting {
  import c.universe._

  def apply[T](implicit t: WeakTypeTag[T]) =
    attach[Queryable[T]](ast.Table(t.tpe.typeSymbol.name.toString): ast.Query)
}