package io.getquill.norm

import io.getquill.ast.Ast

object Normalize {

  def apply(ast: Ast) = AdHocReduction(SymbolicReduction(ast))
}
