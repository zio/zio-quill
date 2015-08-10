package io.getquill.norm.select

import io.getquill.ast._

private[select] object ReplaceSelect {

  def apply(query: Query, exprs: List[Expr]): Query =
    apply(query, Tuple(exprs))

  private def apply(query: Query, expr: Expr): Query =
    query match {
      case FlatMap(q, x, p) => FlatMap(q, x, apply(p, expr))
      case Map(q, x, p)     => Map(q, x, expr)
      case other            => other
    }
}