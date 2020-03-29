package io.getquill.dsl

import io.getquill.util.MacroContextExt._
import scala.reflect.macros.blackbox.{ Context => MacroContext }

class QueryDslMacro(val c: MacroContext) {

  import c.universe._

  def expandEntity[T](implicit t: WeakTypeTag[T]): Tree =
    q"${meta[T]("Schema")}.entity"

  def expandInsert[T](value: Tree)(implicit t: WeakTypeTag[T]): Tree =
    expandAction(value, "Insert")

  def expandUpdate[T](value: Tree)(implicit t: WeakTypeTag[T]): Tree =
    expandAction(value, "Update")

  private def expandAction[T](value: Tree, prefix: String)(implicit t: WeakTypeTag[T]) =
    q"${meta(prefix)}.expand(${c.prefix}, $value)"

  private def meta[T](prefix: String)(implicit t: WeakTypeTag[T]): Tree = {
    val expanderTpe = c.typecheck(tq"io.getquill.dsl.MetaDsl#${TypeName(s"${prefix}Meta")}[$t]", c.TYPEmode)
    c.inferImplicitValue(expanderTpe.tpe, silent = true) match {
      case EmptyTree => c.fail(s"Can't find an implicit `${prefix}Meta` for type `${t.tpe}`")
      case tree      => tree
    }
  }
}
