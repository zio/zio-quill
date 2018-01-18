package io.getquill.quotation

import io.getquill.ast
import io.getquill.ast.{ CollectAst, DynamicName, Entity }

object IsDynamic {
  def apply(a: ast.Ast) =
    ast.CollectAst(a) { case d: Dynamic => d }.nonEmpty

  def typed(a: ast.Ast): DynamicType = {
    val list = CollectAst(a) {
      case d: Dynamic                    => d
      case e @ Entity(_: DynamicName, _) => e
    }
    if (list.isEmpty) None
    else if (list.exists(_.isInstanceOf[Dynamic])) Ast
    else Name
  }

  trait DynamicType
  case object None extends DynamicType
  case object Name extends DynamicType
  case object Ast extends DynamicType
}
