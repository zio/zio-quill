package io.getquill.norm

import io.getquill.ast.And
import io.getquill.ast.Filter
import io.getquill.ast.FlatMap
import io.getquill.ast.Ident
import io.getquill.ast.Map
import io.getquill.ast.Query
import io.getquill.ast.Table

object Normalize {

  def apply(q: Query): Query =
    q match {

      // ************Symbolic***************

      // **for-yld**
      // q.map(x => r).flatMap(y => p) =>
      //    q.flatMap(x => p')
      case FlatMap(Map(q, x, r), y, p) =>
        val pr = BetaReduction(p, y -> r)
        apply(FlatMap(apply(q), x, apply(pr)))

      // **if-yld**
      // q.map(x => r).filter(y => p) =>
      //    q.filter(x => p')
      case Filter(Map(q, x, r), y, p) =>
        val pr = BetaReduction(p, y -> r)
        apply(Filter(apply(q), x, pr))

      // **yld-yld**
      // q.map(x => r).map(y => p) =>
      //    q.map(x => p')
      case Map(Map(q, x, r), y, p) =>
        val pr = BetaReduction(p, y -> r)
        apply(Map(apply(q), x, pr))

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

      // ************AdHoc***************

      // **if-if**
      // r.filter(x => q).filter(y => p) =>
      //    r.filter(x => q && p')
      case Filter(Filter(r, x, q), y, p) =>
        val pr = BetaReduction(p, y -> x)
        apply(Filter(apply(r), x, And(q, pr)))

      // **if-for**
      // q.flatMap(x => r).filter(y => p) =>
      //    q.flatMap(x => r.filter(y => p'))
      case Filter(FlatMap(q, x, r), y, p) =>
        val pr = BetaReduction(p, y -> x)
        apply(FlatMap(apply(q), x, apply(Filter(apply(r), Ident("temp"), pr))))

      // ************Recursion***************

      case FlatMap(q, x, p) => FlatMap(apply(q), x, apply(p))
      case Filter(q, x, p)  => Filter(apply(q), x, p)
      case Map(q, x, p)     => Map(apply(q), x, p)
      case t: Table         => t
    }
}
