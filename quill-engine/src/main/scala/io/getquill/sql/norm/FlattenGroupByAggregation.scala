package io.getquill.context.sql.norm

import io.getquill.ast.Aggregation
import io.getquill.ast.Ast
import io.getquill.ast.Drop
import io.getquill.ast.Filter
import io.getquill.ast.FlatMap
import io.getquill.ast.Ident
import io.getquill.ast.Join
import io.getquill.ast.Map
import io.getquill.ast.Query
import io.getquill.ast.SortBy
import io.getquill.ast.StatelessTransformer
import io.getquill.ast.Take
import io.getquill.ast.Union
import io.getquill.ast.UnionAll
import io.getquill.norm.BetaReduction
import io.getquill.util.Messages.fail
import io.getquill.ast.ConcatMap

case class FlattenGroupByAggregation(agg: Ident) extends StatelessTransformer {

  override def apply(ast: Ast) =
    ast match {
      case q: Query if (isGroupByAggregation(q)) =>
        q match {
          // In a groupBy typically there's a query mapped to an aggregator
          // e.g. people.groupBy(p=>p.name).map((name,people)=>people.map(p=>p.age).max))
          // This part takes the:
          //   people.map(p=>p.age).max
          // which is:
          //   Max(Map(agg:people,p,p.age))
          // which is:
          //   Agg(Max,Map(agg:people,p,p.age))
          // now we:
          //   Reduce(p in p.age)(as: p -> people) which yields people.age
          // and we return:
          //   Agg(Max(people.age))
          // Ultimately this goes into the SQL Select clause (in the SqlQuery class):
          // SELECT Agg(Max(people.age))
          case Aggregation(op, Map(`agg`, ident, body)) =>
            Aggregation(op, BetaReduction(body, ident -> agg))
          case Map(`agg`, ident, body) =>
            BetaReduction(body, ident -> agg)
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
      case Aggregation(a, b)         => isGroupByAggregation(b)
      case Map(a, b, c)              => isGroupByAggregation(a)
      case FlatMap(a, b, c)          => isGroupByAggregation(a)
      case ConcatMap(a, b, c)        => isGroupByAggregation(a)
      case Filter(a, b, c)           => isGroupByAggregation(a)
      case SortBy(a, b, c, d)        => isGroupByAggregation(a)
      case Take(a, b)                => isGroupByAggregation(a)
      case Drop(a, b)                => isGroupByAggregation(a)
      case Union(a, b)               => isGroupByAggregation(a) || isGroupByAggregation(b)
      case UnionAll(a, b)            => isGroupByAggregation(a) || isGroupByAggregation(b)
      case Join(t, a, b, ta, tb, on) => isGroupByAggregation(a) || isGroupByAggregation(b)
      case `agg`                     => true
      case other                     => false
    }

}
