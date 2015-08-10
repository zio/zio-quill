package io.getquill.norm

import io.getquill.ast.`&&`
import io.getquill.ast.Action
import io.getquill.ast.BinaryOperation
import io.getquill.ast.Delete
import io.getquill.ast.Filter
import io.getquill.ast.FlatMap
import io.getquill.ast.Ident
import io.getquill.ast.Insert
import io.getquill.ast.Map
import io.getquill.ast.Query
import io.getquill.ast.Table
import io.getquill.ast.Update

object Normalize {

  def apply(q: Query) = norm(AvoidCapture(q))

  def apply(a: Action): Action =
    a match {
      case Insert(query, assignments) =>
        Insert(norm(query), assignments)
      case Update(query, assignments) =>
        Update(norm(query), assignments)
      case Delete(query) =>
        Delete(norm(query))
    }

  private def norm(q: Query): Query =
    q match {

      // ************Symbolic***************

      // **for-yld**
      // q.map(x => r).flatMap(y => p) =>
      //    q.flatMap(x => p')
      case FlatMap(Map(q, x, r), y, p) =>
        val pr = BetaReduction(p, y -> r)
        norm(FlatMap(norm(q), x, norm(pr)))

      // **if-yld**
      // q.map(x => r).filter(y => p) =>
      //    q.filter(x => p')
      case Filter(Map(q, x, r), y, p) =>
        val pr = BetaReduction(p, y -> r)
        norm(Filter(norm(q), x, pr))

      // **yld-yld**
      // q.map(x => r).map(y => p) =>
      //    q.map(x => p')
      case Map(Map(q, x, r), y, p) =>
        val pr = BetaReduction(p, y -> r)
        norm(Map(norm(q), x, pr))

      // **for-for**
      // p.flatMap(x => q).flatMap(y => r) =>
      //     p.flatMap(x => q.flatMap(y => r))
      case FlatMap(FlatMap(p, x, q), y, r) =>
        norm(FlatMap(norm(p), x, norm(FlatMap(norm(q), y, norm(r)))))

      // **for-if**
      // q.filter(x => p).flatMap(y => r) =>
      //     q.flatMap(y => r).filter(x => p)
      case FlatMap(Filter(q, x, p), y, r) =>
        norm(Filter(norm(FlatMap(norm(q), y, norm(r))), x, p))

      // ************AdHoc***************

      // **if-if**
      // r.filter(x => q).filter(y => p) =>
      //    r.filter(x => q && p')
      case Filter(Filter(r, x, q), y, p) =>
        val pr = BetaReduction(p, y -> x)
        norm(Filter(norm(r), x, BinaryOperation(q, `&&`, pr)))

      // **if-for**
      // q.flatMap(x => r).filter(y => p) =>
      //    q.flatMap(x => r.filter(y => p'))
      case Filter(FlatMap(q, x, r), y, p) =>
        val pr = BetaReduction(p, y -> x)
        norm(FlatMap(norm(q), x, norm(Filter(norm(r), Ident("temp"), pr))))

      // ************Recursion***************

      case FlatMap(q, x, p) => FlatMap(norm(q), x, norm(p))
      case Filter(q, x, p)  => Filter(norm(q), x, p)
      case Map(q, x, p)     => Map(norm(q), x, p)
      case t: Table         => t
    }
}
