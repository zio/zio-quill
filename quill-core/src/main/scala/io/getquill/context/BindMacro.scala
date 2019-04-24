package io.getquill.context

import scala.reflect.macros.whitebox.{ Context => MacroContext }
import io.getquill.util.EnableReflectiveCalls

class BindMacro(val c: MacroContext) extends ContextMacro {
  import c.universe.{ Ident => _, _ }

  def bindQuery[T](quoted: Tree)(implicit t: WeakTypeTag[T]): Tree =
    c.untypecheck {
      q"""
        ..${EnableReflectiveCalls(c)}
        val expanded = ${expand(extractAst(quoted))}
        ${c.prefix}.bindQuery(
          expanded.string,
          expanded.prepare
        )
      """
    }
}