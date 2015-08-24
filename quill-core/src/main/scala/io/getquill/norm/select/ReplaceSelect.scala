package io.getquill.norm.select

import io.getquill.ast.Ast
import io.getquill.ast.FlatMap
import io.getquill.ast.Map
import io.getquill.ast.Query
import io.getquill.ast.Tuple

private[select] object ReplaceSelect {

  def apply(query: Query, asts: List[Ast]): Query =
    apply(query, Tuple(asts))

  private def apply(query: Query, ast: Ast): Query =
    query match {
      case FlatMap(q, x, p: Query) => FlatMap(q, x, apply(p, ast))
      case Map(q, x, p)            => Map(q, x, ast)
      case other                   => other
    }
}
