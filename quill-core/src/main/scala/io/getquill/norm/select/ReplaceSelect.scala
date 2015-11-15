package io.getquill.norm.select

import io.getquill.ast._
import io.getquill.util.Messages.fail

private[select] object ReplaceSelect {

  def apply(query: Query, asts: List[Ast]): Query =
    asts match {
      case ast :: Nil => apply(query, ast)
      case asts       => apply(query, Tuple(asts))
    }

  private def apply(query: Query, ast: Ast): Query =
    query match {
      case Aggregation(op, q: Query) => Aggregation(op, q)
      case FlatMap(q, x, p: Query)   => FlatMap(q, x, apply(p, ast))
      case Map(q, x, p)              => Map(q, x, ast)
      case other                     => fail(s"Query doesn't have a final map (select). Ast: $query")
    }
}
