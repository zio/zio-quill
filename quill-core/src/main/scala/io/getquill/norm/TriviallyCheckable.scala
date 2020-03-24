package io.getquill.norm

import io.getquill.ast.{ +!=+, +&&+, +==+, +||+, Ast, Constant }

object TriviallyCheckable {

  def unapply(ast: Ast): Option[Ast] = ast match {
    case c @ Constant(_) => Some(c)

    case TriviallyCheckable(Constant(true)) +&&+ TriviallyCheckable(Constant(true)) => Some(Constant(true))
    case TriviallyCheckable(Constant(false)) +&&+ _ => Some(Constant(false))
    case _ +&&+ TriviallyCheckable(Constant(false)) => Some(Constant(false))

    case TriviallyCheckable(Constant(true)) +||+ _ => Some(Constant(true))
    case _ +||+ TriviallyCheckable(Constant(true)) => Some(Constant(true))
    case TriviallyCheckable(Constant(false)) +||+ TriviallyCheckable(Constant(false)) => Some(Constant(false))

    case TriviallyCheckable(one) +==+ TriviallyCheckable(two) if (one == two) => Some(Constant(true))
    case TriviallyCheckable(one) +!=+ TriviallyCheckable(two) if (one != two) => Some(Constant(true))
    case TriviallyCheckable(one) +==+ TriviallyCheckable(two) if (one != two) => Some(Constant(false))
    case TriviallyCheckable(one) +!=+ TriviallyCheckable(two) if (one == two) => Some(Constant(false))

    case _ => None
  }
}
