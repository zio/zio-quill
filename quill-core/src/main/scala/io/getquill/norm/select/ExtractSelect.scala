package io.getquill.norm.select

import io.getquill.ast.Ast
import io.getquill.ast.Entity
import io.getquill.ast.Filter
import io.getquill.ast.FlatMap
import io.getquill.ast.Ident
import io.getquill.ast.Map
import io.getquill.ast.Query
import io.getquill.util.Messages.fail
import io.getquill.ast.SortBy

private[select] object ExtractSelect {

  def apply(query: Query): (Query, Ast) =
    query match {
      case FlatMap(q, x, p: Query) =>
        val (pr, map) = apply(p)
        (FlatMap(q, x, pr), map)
      case SortBy(q: Query, x, p) =>
        val (qr, map) = apply(q)
        (SortBy(qr, x, p), map)
      case q @ Filter(_, x, _) =>
        (Map(q, x, x), x)
      case t: Entity =>
        val x = Ident("x")
        (Map(t, x, x), x)
      case Map(q, x, p) =>
        (query, p)
      case other =>
        fail(s"Can't find the final map (select) in $query")
    }
}
