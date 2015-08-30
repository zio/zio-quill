package io.getquill.norm

import io.getquill.ast.`&&`
import io.getquill.ast.Ast
import io.getquill.ast.BinaryOperation
import io.getquill.ast.Entity
import io.getquill.ast.Filter
import io.getquill.ast.FlatMap
import io.getquill.ast.Ident
import io.getquill.ast.Map
import io.getquill.ast.Query
import io.getquill.ast.StatelessTransformer
import io.getquill.norm.capture.AvoidCapture
import io.getquill.ast.SortBy
import io.getquill.ast.Tuple

object AdHocReduction extends StatelessTransformer {

  override def apply(q: Query): Query =
    q match {

      // ************Nested***************

      case Map(a, b, c) if (apply(a) != a || apply(c) != c) =>
        apply(Map(apply(a), b, apply(c)))

      case FlatMap(a, b, c) if (apply(a) != a || apply(c) != c) =>
        apply(FlatMap(apply(a), b, apply(c)))

      case Filter(a, b, c) if (apply(a) != a || apply(c) != c) =>
        apply(Filter(apply(a), b, apply(c)))

      case SortBy(a, b, c) if (apply(a) != a || apply(c) != c) =>
        apply(SortBy(apply(a), b, apply(c)))

      // ---------------------------
      // order sortBy and filter

      // a.sortBy(b => c).filter(d => e) =>
      //     a.filter(d => e).sortBy(b => c)
      case Filter(SortBy(a, b, c), d, e) =>
        apply(SortBy(Filter(a, d, e), b, c))

      // ---------------------------
      // combine multiple filters and sortbys

      // a.sortBy(b => (c)).sortBy(d => e) =>
      //    a.sortBy(b => (c, e[d := b]))
      case SortBy(SortBy(a, b, Tuple(c)), d, e) =>
        val er = BetaReduction(e, d -> b)
        apply(SortBy(a, b, Tuple(c :+ er)))

      // a.sortBy(b => c).sortBy(d => e) =>
      //    a.sortBy(b => (c, e[d := b]))
      case SortBy(SortBy(a, b, c), d, e) =>
        val er = BetaReduction(e, d -> b)
        apply(SortBy(a, b, Tuple(List(c, er))))

      // a.filter(b => c).filter(d => e) =>
      //    a.filter(b => c && e[d := b])
      case Filter(Filter(a, b, c), d, e) =>
        val er = BetaReduction(e, d -> b)
        apply(Filter(a, b, BinaryOperation(c, `&&`, er)))

      // ---------------------------
      // move final sortbys and maps to inside the base flatmap's body

      // a.flatMap(b => c).map(d => e) =>
      //    a.flatMap(b => c.map(d => e))
      case Map(FlatMap(a, b, c), d, e) =>
        apply(FlatMap(a, b, Map(c, d, e)))

      // a.flatMap(b => c).filter(d => e) =>
      //    a.flatMap(b => c.filter(d => e))
      case Filter(FlatMap(a, b, c), d, e) =>
        apply(FlatMap(a, b, Filter(c, d, e)))

      // a.flatMap(b => c).sortBy(d => e) =>
      //    a.flatMap(b => c.sortBy(d => e))
      case SortBy(FlatMap(a, b, c), d, e) =>
        apply(FlatMap(a, b, SortBy(c, d, e)))

      case other => other
    }
}
