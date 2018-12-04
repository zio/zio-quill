package io.getquill.quotation

import io.getquill.ast.{ Ast, CollectAst, Dynamic, Splice }

sealed trait IsDynamic

object IsDynamic {
  case object Yes extends IsDynamic
  case object No extends IsDynamic
  case object Partially extends IsDynamic

  def apply(a: Ast) = {
    val set = CollectAst(a) {
      case _: Dynamic => 1
      case _: Splice  => 2
    }.toSet

    if (set.contains(1)) Yes
    else if (set.contains(2)) Partially
    else No
  }

}
