package io.getquill.norm.select

import io.getquill.ast.Ast
import io.getquill.ast.FlatMap
import io.getquill.ast.Ident
import io.getquill.ast.Map
import io.getquill.ast.Query
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
      case q =>
        val x = Ident("x")
        Map(q, x, x)
    }

  private def extractSelect(query: Query): Ast =
    (query: @unchecked) match {
      case Map(q, x, p) =>
        p
      case FlatMap(q, x, p: Query) =>
        extractSelect(p)
    }
}
