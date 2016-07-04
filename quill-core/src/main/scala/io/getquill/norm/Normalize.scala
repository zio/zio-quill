package io.getquill.norm

import io.getquill.ast.Ast
import io.getquill.ast.Query
import io.getquill.ast.StatelessTransformer
import io.getquill.norm.capture.AvoidCapture

object Normalize extends StatelessTransformer {

  override def apply(q: Ast): Ast =
    super.apply(BetaReduction(q))

  override def apply(q: Query): Query =
    norm(AvoidCapture(q))

  private def norm(q: Query): Query =
    q match {
      case NormalizeNestedStructures(query) => norm(query)
      case ApplyIntermediateMap(query)      => norm(query)
      case SymbolicReduction(query)         => norm(query)
      case AdHocReduction(query)            => norm(query)
      case OrderTerms(query)                => norm(query)
      case other                            => other
    }
}
