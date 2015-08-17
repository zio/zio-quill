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
import io.getquill.norm.capture.AvoidCapture
import io.getquill.ast.SimpleTransformer

object Normalize extends SimpleTransformer {

  def apply(a: Action): Action =
    a match {
      case Insert(query, assignments) =>
        Insert(apply(query), assignments)
      case Update(query, assignments) =>
        Update(apply(query), assignments)
      case Delete(query) =>
        Delete(apply(query))
    }

  override def apply(q: Query): Query =
    AvoidCapture(q) match {

      // ************Symbolic***************

      // a.flatMap(b => c.map(d => e)).flatMap(f => g)
      //    a.flatMap(b => c).flatMap(d => g[f := e])
      case FlatMap(FlatMap(a, b, Map(c, d, e)), f, g) =>
        val gr = BetaReduction(g, f -> e)
        apply(FlatMap(FlatMap(a, b, c), d, gr))

      // a.map(b => c).flatMap(d => e) =>
      //    a.flatMap(b => e[d := c])
      case FlatMap(Map(a, b, c), d, e) =>
        val er = BetaReduction(e, d -> c)
        apply(FlatMap(a, b, er))

      // a.map(b => c).filter(d => e) =>
      //    a.filter(b => e[d := c]).map(b => c)
      case Filter(Map(a, b, c), d, e) =>
        val er = BetaReduction(e, d -> c)
        apply(Map(Filter(a, b, er), b, c))

      // a.map(b => c).map(d => e) =>
      //    a.map(b => e[d := c])
      case Map(Map(a, b, c), d, e) =>
        val er = BetaReduction(e, d -> c)
        apply(Map(a, b, er))

      // a.flatMap(b => c).flatMap(d => e) =>
      //     a.flatMap(b => c.flatMap(d => e))
      case FlatMap(FlatMap(a, b, c), d, e) =>
        apply(FlatMap(a, b, FlatMap(c, d, e)))

      // a.filter(b => c).flatMap(d => e.map(f => g)) =>
      //     a.flatMap(d => e.filter(temp => c[b := d]).map(f => g))
      case FlatMap(Filter(a, b, c), d, Map(e, f, g)) =>
        val cr = BetaReduction(c, b -> d)
        apply(FlatMap(a, d, Map(Filter(e, Ident("temp"), cr), f, g)))

      // a.filter(b => c).flatMap(d => e) =>
      //     a.flatMap(d => e.filter(temp => c[b := d]))
      case FlatMap(Filter(a, b, c), d, e) =>
        val cr = BetaReduction(c, b -> d)
        apply(FlatMap(a, d, Filter(e, Ident("temp"), cr)))

      // ************AdHoc***************

      // a.filter(b => c).filter(d => e) =>
      //    a.filter(b => c && e[d := b])
      case Filter(Filter(a, b, c), d, e) =>
        val er = BetaReduction(e, d -> b)
        apply(Filter(a, b, BinaryOperation(c, `&&`, er)))

      // a.flatMap(b => c).filter(d => e) =>
      //    a.flatMap(b => c.filter(temp => e[d := b]))
      case Filter(FlatMap(a, b, c), d, e) =>
        val er = BetaReduction(e, d -> b)
        apply(FlatMap(a, b, Filter(c, Ident("temp"), er)))

      // ************Recursion***************

      case FlatMap(a, b, c) =>
        (apply(a), apply(c)) match {
          case (`a`, `c`) => FlatMap(a, b, c)
          case (a, c)     => apply(FlatMap(a, b, c))
        }
      case Filter(a, b, c) =>
        apply(a) match {
          case `a` => Filter(a, b, c)
          case a   => apply(Filter(a, b, c))
        }
      case Map(a, b, c) =>
        apply(a) match {
          case `a` => Map(a, b, c)
          case a   => apply(Map(a, b, c))
        }
      case t: Table => t
    }
}
