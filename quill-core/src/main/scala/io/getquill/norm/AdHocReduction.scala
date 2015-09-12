package io.getquill.norm

import io.getquill.ast
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
        Some(Filter(a, b, BinaryOperation(c, ast.`&&`, er)))

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
      case SortBy(FlatMap(a, b, c), d, e) =>
        Some(FlatMap(a, b, SortBy(c, d, e)))

      // a.flatMap(b => c).reverse =>
      //    a.flatMap(b => c.reverse)
      case Reverse(FlatMap(a, b, c)) =>
        Some(FlatMap(a, b, Reverse(c)))

      case other => None
    }

}
