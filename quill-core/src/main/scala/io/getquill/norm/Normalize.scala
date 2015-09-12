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
import io.getquill.ast.Ast

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
}
