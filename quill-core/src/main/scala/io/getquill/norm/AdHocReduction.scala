package io.getquill.norm

import io.getquill.ast
import io.getquill.ast.BinaryOperation
import io.getquill.ast.Filter
import io.getquill.ast.FlatMap
import io.getquill.ast.Map
import io.getquill.ast.Query
import io.getquill.ast.SortBy
import io.getquill.ast.StatelessTransformer
import io.getquill.ast.Tuple
import io.getquill.ast.Reverse
import io.getquill.norm.capture.AvoidCapture

private[norm] object AdHocReduction extends StatelessTransformer {

  private val reduceNestedStructures = ReduceNestedStructures(apply)

  override def apply(q: Query): Query =
    AvoidCapture(q) match {

      case `reduceNestedStructures`(query) =>
        apply(query)

      case ApplyIntermediateMap(query) =>
        apply(query)

      // ---------------------------
      // reverse

      // a.reverse.reverse =>
      //    a
      case Reverse(Reverse(a: Query)) =>
        apply(a)

      // a.reverse.filter(b => c) =>
      //     a.filter(b => c).reverse
      case Filter(Reverse(a), b, c) =>
        apply(Reverse(Filter(a, b, c)))

      // a.map(b => c).reverse =>
      //     a.reverse.map(b => c)
      case Reverse(Map(a, b, c)) =>
        apply(Map(Reverse(a), b, c))

      // ---------------------------
      // sortBy

      // a.sortBy(b => c).filter(d => e) =>
      //     a.filter(d => e).sortBy(b => c)
      case Filter(SortBy(a, b, c), d, e) =>
        apply(SortBy(Filter(a, d, e), b, c))

      // ---------------------------
      // filter

      // a.filter(b => c).filter(d => e) =>
      //    a.filter(b => c && e[d := b])
      case Filter(Filter(a, b, c), d, e) =>
        val er = BetaReduction(e, d -> b)
        apply(Filter(a, b, BinaryOperation(c, ast.`&&`, er)))

      // ---------------------------
      // flatMap

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

      // a.flatMap(b => c).reverse =>
      //    a.flatMap(b => c.reverse)
      case Reverse(FlatMap(a, b, c)) =>
        apply(FlatMap(a, b, Reverse(c)))

      case other => other
    }
}
