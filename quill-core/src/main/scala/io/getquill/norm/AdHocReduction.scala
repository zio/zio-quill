package io.getquill.norm

import io.getquill.ast._

object AdHocReduction {

  def unapply(q: Query) =
    q match {

      // ---------------------------
      // filter.filter

      // a.filter(b => c).filter(d => e) =>
      //    a.filter(b => c && e[d := b])
      case Filter(Filter(a, b, c), d, e) =>
        val er = BetaReduction(e, d -> b)
        Some(Filter(a, b, BinaryOperation(c, BooleanOperator.`&&`, er)))

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
