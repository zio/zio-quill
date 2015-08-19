package io.getquill.norm.capture

import io.getquill.ast._
import io.getquill.ast.Query
import io.getquill.norm.BetaReduction

private[capture] object Dealias extends StatelessTransformer {

  override def apply(e: Query): Query =
    e match {

      case FlatMap(Filter(q, x, p), y, r) =>
        val rr = BetaReduction(r, y -> x)
        FlatMap(apply(Filter(q, x, p)), x, apply(rr))

      case Filter(Filter(q, x, p), y, r) =>
        val rr = BetaReduction(r, y -> x)
        Filter(apply(Filter(q, x, p)), x, rr)

      case Map(Filter(q, x, p), y, r) =>
        val rr = BetaReduction(r, y -> x)
        Map(apply(Filter(q, x, p)), x, rr)

      case other => super.apply(other)
    }
}
