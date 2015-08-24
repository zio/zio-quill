package io.getquill.norm.select

import io.getquill.ast.Ast
import io.getquill.ast.Filter
import io.getquill.ast.FlatMap
import io.getquill.ast.Ident
import io.getquill.ast.Map
import io.getquill.ast.Query
import io.getquill.ast.Entity
import io.getquill.util.Messages.fail

private[select] object ExtractSelect {

  def apply(q: Query) = {
    val query = ensureFinalMap(q)
    (query, mapAst(query))
  }

  private def ensureFinalMap(query: Query): Query =
    query match {
      case FlatMap(q, x, p: Query) => FlatMap(q, x, ensureFinalMap(p))
      case q @ Filter(_, x, _)     => Map(q, x, x)
      case t: Entity                => Map(t, Ident("x"), Ident("x"))
      case other                   => query
    }

  private def mapAst(query: Query): Ast =
    query match {
      case FlatMap(q, x, p: Query) => mapAst(p)
      case Map(q, x, p)            => p
      case other                   => fail(s"Query not properly normalized, please submit a bug report. $other")
    }
}