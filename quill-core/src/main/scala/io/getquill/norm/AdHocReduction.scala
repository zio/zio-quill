package io.getquill.norm

import io.getquill.ast._
import io.getquill.ast.Filter
import io.getquill.ast.FlatMap
import io.getquill.ast.Query

private[norm] object AdHocReduction {

  def apply(q: Query): Query =
    q match {

      // **if-if**
      // r.filter(x => q).filter(y => p) =>
      //    r.filter(x => q && p')
      case Filter(Filter(r, x, q), y, p) =>
        val pr = BetaReduction(p)(collection.Map(y -> x))
        apply(Filter(apply(r), x, And(q, pr)))

      // **if-for**
      // q.flatMap(x => r).filter(y => p) =>
      //    q.flatMap(x => r.filter(y => p'))
      case Filter(FlatMap(q, x, r), y, p) =>
        val pr = BetaReduction(p)(collection.Map(y -> x))
        apply(FlatMap(apply(q), x, apply(Filter(apply(r), y, pr))))

      // **if-yld**
      // q.map(x => r).filter(y => p) =>
      //    q.filter(y => p).map(x => r)
      case Filter(Map(q, x, r), y, p) =>
        apply(Map(Filter(apply(q), y, p), x, r))

      case FlatMap(q, x, p) =>
        FlatMap(apply(q), x, apply(p))

      case Filter(q, x, p) =>
        Filter(apply(q), x, p)

      case Map(q, x, p) =>
        Map(apply(q), x, p)

      case t: Table => t
    }
}
