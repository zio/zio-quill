package io.getquill.quotation

import io.getquill.ast.Ast
import io.getquill.ast.CollectAst
import io.getquill.ast.Dynamic

object IsDynamic {
  def apply(a: Ast) =
    CollectAst(a) { case d: Dynamic => d }.nonEmpty
}
