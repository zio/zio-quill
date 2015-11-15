package io.getquill.norm.select

import io.getquill.ast._
import io.getquill.norm.capture.Dealias

private[select] object ExtractSelect {

  def apply(query: Query) = {
    val q = Dealias(ensureFinalMap(query))
    (q, extractSelect(q))
  }

  private def ensureFinalMap(query: Query): Query =
    query match {
      case Map(q, x, p) =>
        query
      case FlatMap(q, x, p: Query) =>
        FlatMap(q, x, ensureFinalMap(p))
      case Aggregation(op, q: Query) =>
        Aggregation(op, q)
      case q =>
        val x = Ident("x")
        Map(q, x, x)
    }

  private def extractSelect(query: Query): Ast =
    (query: @unchecked) match {
      case Aggregation(op, q: Query) =>
        Ident("x")
      case Map(q, x, p) =>
        p
      case FlatMap(q, x, p: Query) =>
        extractSelect(p)
    }
}
