package io.getquill.norm

import io.getquill.ast.`&&`
import io.getquill.ast.Ast
import io.getquill.ast.BinaryOperation
import io.getquill.ast.Entity
import io.getquill.ast.Filter
import io.getquill.ast.FlatMap
import io.getquill.ast.Ident
import io.getquill.ast.Map
import io.getquill.ast.Query
import io.getquill.ast.StatelessTransformer
import io.getquill.norm.capture.AvoidCapture
import io.getquill.ast.SortBy
import io.getquill.ast.Tuple

object Normalize extends StatelessTransformer {

  override def apply(q: Ast): Ast =
    super.apply(BetaReduction(q))

  override def apply(q: Query): Query =
    BetaReduction {
      AvoidCapture(q) match {

        // ************Symbolic***************

        // ---------- map.* -------------

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

        // a.map(b => c).sortBy(d => e) =>
        //    a.sortBy(b => e[d := c]).map(b => c)
        case SortBy(Map(a, b, c), d, e) =>
          val er = BetaReduction(e, d -> c)
          apply(Map(SortBy(a, b, er), b, c))

        // -------- *.flatMap -----------

        // a.flatMap(b => c.map(d => e)).flatMap(f => g)
        //    a.flatMap(b => c).flatMap(d => g[f := e])
        case FlatMap(FlatMap(a, b, Map(c, d, e)), f, g) =>
          val gr = BetaReduction(g, f -> e)
          apply(FlatMap(FlatMap(a, b, c), d, gr))

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

        // a.sortBy(b => c).flatMap(d => e.map(f => g)) =>
        //     a.flatMap(d => e.sortBy(temp => c[b := d]).map(f => g))
        case FlatMap(SortBy(a, b, c), d, Map(e, f, g)) =>
          val cr = BetaReduction(c, b -> d)
          apply(FlatMap(a, d, Map(SortBy(e, Ident("temp"), cr), f, g)))

        // a.sortBy(b => c).flatMap(d => e) =>
        //     a.flatMap(d => e.sortBy(temp => c[b := d]))
        case FlatMap(SortBy(a, b, c), d, e) =>
          val cr = BetaReduction(c, b -> d)
          apply(FlatMap(a, d, SortBy(e, Ident("temp"), cr)))

        // ************AdHoc***************

        // -------- filter -----------

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

        // a.sortBy(b => c).filter(d => e) =>
        //     a.filter(d => e).sortBy(b => c)
        case Filter(SortBy(a, b, c), d, e) =>
          apply(SortBy(Filter(a, d, e), b, c))

        // -------- sortBy -----------

        // a.sortBy(b => (c)).sortBy(d => e) =>
        //    a.sortBy(b => (c, e[d := b]))
        case SortBy(SortBy(a, b, Tuple(c)), d, e) =>
          val er = BetaReduction(e, d -> b)
          apply(SortBy(a, b, Tuple(c :+ er)))

        // a.sortBy(b => c).sortBy(d => e) =>
        //    a.sortBy(b => (c, e[d := b]))
        case SortBy(SortBy(a, b, c), d, e) =>
          val er = BetaReduction(e, d -> b)
          apply(SortBy(a, b, Tuple(List(c, er))))

        // a.flatMap(b => c).sortBy(d => e) =>
        //    a.flatMap(b => c.sortBy(temp => e[d := b]))
        case SortBy(FlatMap(a, b, c), d, e) =>
          val er = BetaReduction(e, d -> b)
          apply(FlatMap(a, b, SortBy(c, Ident("temp"), er)))

        // ************Recursion***************

        case FlatMap(a, b, c) =>
          (apply(a), apply(c)) match {
            case (`a`, `c`) => FlatMap(a, b, c)
            case (a, c)     => apply(FlatMap(apply(a), b, apply(c)))
          }
        case Filter(a, b, c) =>
          (apply(a), apply(c)) match {
            case (`a`, `c`) => Filter(a, b, c)
            case (a, c)     => apply(Filter(apply(a), b, apply(c)))
          }
        case Map(a, b, c) =>
          (apply(a), apply(c)) match {
            case (`a`, `c`) => Map(a, b, c)
            case (a, c)     => apply(Map(apply(a), b, apply(c)))
          }

        case SortBy(a, b, c) =>
          (apply(a), apply(c)) match {
            case (`a`, `c`) => SortBy(a, b, c)
            case (a, c)     => apply(SortBy(apply(a), b, apply(c)))
          }

        case t: Entity => t
      }
    }
}
