package io.getquill.norm

import io.getquill.ast._
import io.getquill.norm.capture.AvoidCapture

object Normalize extends StatelessTransformer {

  override def apply(q: Ast): Ast =
    super.apply(BetaReduction(q))

  override def apply(q: Query): Query =
    AvoidCapture(q) match {
      case NormalizeNestedStructures(query) => apply(query)
      case ApplyIntermediateMap(query)      => apply(query)
      case SymbolicReduction(query)         => apply(query)
      case AdHocReduction(query)            => apply(query)
      case OrderTerms(query)                => apply(query)
      case other                            => other
    }

  override def apply(q: OuterJoin): OuterJoin = super.apply(q)
}
