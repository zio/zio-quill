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

  def apply(a: Action): Action =
    a match {
      case Insert(query, assignments) =>
        Insert(apply(query), assignments)
      case Update(query, assignments) =>
        Update(apply(query), assignments)
      case Delete(query) =>
        Delete(apply(query))
    }

  def apply(q: Query) =
    BetaReduction(norm(AvoidCapture(q)))(collection.Map.empty)

  private def norm(q: Query): Query =
    q match {

      // ************Symbolic***************

      // a.flatMap(b => c).flatMap(d => e) =>
      //     a.flatMap(b => c.flatMap(d => e))
      case FlatMap(FlatMap(a, b, c), d, e) =>
        norm(FlatMap(a, b, FlatMap(c, d, e)))

      // a.filter(b => c).flatMap(d => e) =>
      //     a.flatMap(d => r.filter(temp => c[b := d]))
      case FlatMap(Filter(a, b, c), d, e) =>
        val pr = BetaReduction(c, b -> d)
        norm(FlatMap(a, d, Filter(e, Ident("temp"), pr)))

      // ************AdHoc***************

      // a.filter(b => c).filter(d => e) =>
      //    a.filter(b => c && e[d := b])
      case Filter(Filter(a, b, c), d, e) =>
        val er = BetaReduction(e, d -> b)
        norm(Filter(a, b, BinaryOperation(c, `&&`, er)))

      // a.flatMap(b => c).filter(d => e) =>
      //    a.flatMap(b => c.filter(temp => e[d := b]))
      case Filter(FlatMap(a, b, c), d, e) =>
        val er = BetaReduction(e, d -> b)
        norm(FlatMap(a, b, Filter(c, Ident("temp"), er)))

      // ************Recursion***************

      case FlatMap(a, b, c) => FlatMap(norm(a), b, norm(c))
      case Filter(a, b, c)  => Filter(norm(a), b, c)
      case Map(a, b, c)     => Map(norm(a), b, c)
      case t: Table         => t
    }
}
