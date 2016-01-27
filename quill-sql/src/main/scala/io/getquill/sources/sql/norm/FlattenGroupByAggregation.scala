package io.getquill.sources.sql.norm

import io.getquill.ast.StatelessTransformer
import io.getquill.ast._
import io.getquill.norm.BetaReduction
import io.getquill.util.Messages._

case class FlattenGroupByAggregation(agg: Ident) extends StatelessTransformer {

  override def apply(ast: Ast) =
    ast match {
      case q: Query if (isGroupByAggregation(q)) =>
        q match {
          case Aggregation(op, Map(`agg`, ident, body)) =>
            Aggregation(op, BetaReduction(body, ident -> agg))
          case q @ Aggregation(op, `agg`) =>
            q
          case other =>
            fail(s"Invalid group by aggregation: '$other'")
        }
      case other =>
        super.apply(other)
    }

  private[this] def isGroupByAggregation(ast: Ast): Boolean =
    ast match {
      case Aggregation(a, b)              => isGroupByAggregation(b)
      case Map(a, b, c)                   => isGroupByAggregation(a)
      case FlatMap(a, b, c)               => isGroupByAggregation(a)
      case Filter(a, b, c)                => isGroupByAggregation(a)
      case SortBy(a, b, c, d)             => isGroupByAggregation(a)
      case Take(a, b)                     => isGroupByAggregation(a)
      case Drop(a, b)                     => isGroupByAggregation(a)
      case Union(a, b)                    => isGroupByAggregation(a) || isGroupByAggregation(b)
      case UnionAll(a, b)                 => isGroupByAggregation(a) || isGroupByAggregation(b)
      case Join(t, a, b, ta, tb, on) => isGroupByAggregation(a) || isGroupByAggregation(b)
      case `agg`                          => true
      case other                          => false
    }

}
