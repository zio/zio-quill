package io.getquill.norm

import io.getquill.ast.Ast
import io.getquill.ast.Filter
import io.getquill.ast.FlatMap
import io.getquill.ast.Query
import io.getquill.ast.SortBy
import io.getquill.ast.StatelessTransformer
import io.getquill.norm.capture.AvoidCapture
import io.getquill.ast.Entity
import io.getquill.ast.Reverse

private[norm] object SymbolicReduction extends StatelessTransformer {

  private val reduceNestedStructures = ReduceNestedStructures(apply)

  override def apply(q: Ast): Ast =
    super.apply(BetaReduction(q))

  override def apply(q: Query): Query =
    BetaReduction {
      AvoidCapture(q) match {

        case `reduceNestedStructures`(query) =>
          apply(query)

        case ApplyIntermediateMap(query) =>
          apply(query)

        // ---------------------------
        // *.flatMap

        // a.filter(b => c).flatMap(d => e.$) =>
        //     a.flatMap(d => e.filter(_ => c[b := d]).$)
        case FlatMap(Filter(a, b, c), d, e: Query) =>
          val cr = BetaReduction(c, b -> d)
          val er = AttachToEntity(Filter(_, _, cr))(e)
          apply(FlatMap(a, d, er))

        // a.sortBy(b => c).flatMap(d => e.$) =>
        //     a.flatMap(d => e.sortBy(_ => c[b := d]).$)
        case FlatMap(SortBy(a, b, c), d, e: Query) =>
          val cr = BetaReduction(c, b -> d)
          val er = AttachToEntity(SortBy(_, _, cr))(e)
          apply(FlatMap(a, d, er))

        // a.reverse.flatMap(d => e.$) =>
        //     a.flatMap(d => e.reverse.$)
        case FlatMap(Reverse(a), d, e: Query) =>
          val er = AttachToEntity { case (q, i) => Reverse(q) }(e)
          apply(FlatMap(a, d, er))

        // a.flatMap(b => c).flatMap(d => e) =>
        //     a.flatMap(b => c.flatMap(d => e))
        case FlatMap(FlatMap(a, b, c), d, e) =>
          apply(FlatMap(a, b, FlatMap(c, d, e)))

        case other => other
      }
    }
}
