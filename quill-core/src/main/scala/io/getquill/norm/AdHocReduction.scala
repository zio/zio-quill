package io.getquill.norm

import io.getquill.ast.BinaryOperation
import io.getquill.ast.BooleanOperator
import io.getquill.ast.Filter
import io.getquill.ast.FlatMap
import io.getquill.ast.Join
import io.getquill.ast.Map
import io.getquill.ast.Query
import io.getquill.ast.SortBy
import io.getquill.ast.Tuple
import io.getquill.ast.Union
import io.getquill.ast.UnionAll

object AdHocReduction {

  def unapply(q: Query) =
    q match {

      // ---------------------------
      // *.filter

      // a.filter(b => c).filter(d => e) =>
      //    a.filter(b => c && e[d := b])
      case Filter(Filter(a, b, c), d, e) =>
        val er = BetaReduction(e, d -> b)
        Some(Filter(a, b, BinaryOperation(c, BooleanOperator.`&&`, er)))

      // a.join(b).on((c, d) => e).filter(f => g)
      //    a.join(b).on((c, d) => e && g[f := (c, d)])
      case Filter(Join(t, a, b, c, d, e), f, g) =>
        val gr = BetaReduction(g, f -> Tuple(List(c, d)))
        Some(Join(t, a, b, c, d, BinaryOperation(e, BooleanOperator.`&&`, gr)))

      // ---------------------------
      // flatMap.*

      // a.flatMap(b => c).map(d => e) =>
      //    a.flatMap(b => c.map(d => e))
      case Map(FlatMap(a, b, c), d, e) =>
        Some(FlatMap(a, b, Map(c, d, e)))

      // a.flatMap(b => c).filter(d => e) =>
      //    a.flatMap(b => c.filter(d => e))
      case Filter(FlatMap(a, b, c), d, e) =>
        Some(FlatMap(a, b, Filter(c, d, e)))

      // a.flatMap(b => c).sortBy(d => e) =>
      //    a.flatMap(b => c.sortBy(d => e))
      case SortBy(FlatMap(a, b, c), d, e, f) =>
        Some(FlatMap(a, b, SortBy(c, d, e, f)))

      // a.flatMap(b => c.union(d))
      //    a.flatMap(b => c).union(a.flatMap(b => d))
      case FlatMap(a, b, Union(c, d)) =>
        Some(Union(FlatMap(a, b, c), FlatMap(a, b, d)))

      // a.flatMap(b => c.unionAll(d))
      //    a.flatMap(b => c).unionAll(a.flatMap(b => d))
      case FlatMap(a, b, UnionAll(c, d)) =>
        Some(UnionAll(FlatMap(a, b, c), FlatMap(a, b, d)))

      case other => None
    }

}
