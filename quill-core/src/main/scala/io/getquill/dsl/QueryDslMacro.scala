package io.getquill.dsl

import scala.reflect.macros.blackbox.{ Context => MacroContext }

class QueryDslMacro(val c: MacroContext) {

  import c.universe._

  def expandInsert[T](value: Tree)(implicit t: WeakTypeTag[T]): Tree =
    expandAction(value, "Insert")

  def expandUpdate[T](value: Tree)(implicit t: WeakTypeTag[T]): Tree =
    expandAction(value, "Update")

  private def expandAction[T](value: Tree, prefix: String)(implicit t: WeakTypeTag[T]) =
    q"${meta(prefix)}.expand(${c.prefix}, $value)"

  private def meta[T](prefix: String)(implicit t: WeakTypeTag[T]): Tree = {
    val expanderTpe = c.typecheck(tq"${TypeName(s"${prefix}Meta")}[$t]", c.TYPEmode)
    c.inferImplicitValue(expanderTpe.tpe)
      .orElse(q"${TermName(s"materialize${prefix}Meta")}[$t]")
  }
}
