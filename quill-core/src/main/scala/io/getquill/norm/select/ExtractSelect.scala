package io.getquill.norm.select

import io.getquill.ast._
import io.getquill.util.Messages._

private[select] object ExtractSelect {

  def apply(q: Query) = {
    val query = ensureFinalMap(q)
    (query, mapExpr(query))
  }

  private def ensureFinalMap(query: Query): Query =
    query match {
      case FlatMap(q, x, p)    => FlatMap(q, x, ensureFinalMap(p))
      case q: Map              => query
      case q @ Filter(_, x, _) => Map(q, x, x)
      case t: Table            => Map(t, Ident("x"), Ident("x"))
    }

  private def mapExpr(query: Query): Expr =
    query match {
      case FlatMap(q, x, p) => mapExpr(p)
      case Map(q, x, p)     => p
      case other            => fail(s"Query not properly normalized, please submit a bug report. $other")
    }
}