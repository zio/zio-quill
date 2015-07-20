package io.getquill.norm

import io.getquill.ast.Filter
import io.getquill.ast._
import io.getquill.ast.Map
import io.getquill.ast.Query

private[norm] object SymbolicReduction {

  def apply(q: Query): Query =
    q match {

      // **for-yld**
      // q.map(x => r).flatMap(y => p) =>
      //    q.flatMap(x => p')
      case FlatMap(Map(q, x, r), y, p) =>
        val pr = BetaReduction(p)(collection.Map(y -> r))
        apply(FlatMap(apply(q), x, apply(pr)))

      // **for-for**
      // p.flatMap(x => q).flatMap(y => r) =>
      //     p.flatMap(x => q.flatMap(y => r))
      case FlatMap(FlatMap(p, x, q), y, r) =>
        apply(FlatMap(apply(p), x, apply(FlatMap(apply(q), y, apply(r)))))

      // **for-if**
      // q.filter(x => p).flatMap(y => r) =>
      //     q.flatMap(y => r).filter(x => p)
      case FlatMap(Filter(q, x, p), y, r) =>
        apply(Filter(apply(FlatMap(apply(q), y, apply(r))), x, p))

      case FlatMap(q, x, p) =>
        FlatMap(apply(q), x, apply(p))

      case Filter(q, x, p) =>
        Filter(apply(q), x, p)

      case Map(q, x, p) =>
        Map(apply(q), x, p)

      case t: Table => t
    }
}
