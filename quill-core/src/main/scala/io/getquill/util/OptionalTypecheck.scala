package io.getquill.util

import scala.reflect.macros.blackbox.{ Context => MacroContext }

object OptionalTypecheck {

  def apply(c: MacroContext)(tree: c.Tree): Option[c.Tree] =
    c.typecheck(tree, silent = true) match {
      case c.universe.EmptyTree => None
      case tree                 => Some(tree)
    }
}
