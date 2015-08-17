package io.getquill.norm.select

import io.getquill.ast.Expr
import io.getquill.ast.FlatMap
import io.getquill.ast.Map
import io.getquill.ast.Query
import io.getquill.ast.Tuple

private[select] object ReplaceSelect {

  def apply(query: Query, exprs: List[Expr]): Query =
    apply(query, Tuple(exprs))

  private def apply(query: Query, expr: Expr): Query =
    query match {
      case FlatMap(q, x, p: Query) => FlatMap(q, x, apply(p, expr))
      case Map(q, x, p)            => Map(q, x, expr)
      case other                   => other
    }
}