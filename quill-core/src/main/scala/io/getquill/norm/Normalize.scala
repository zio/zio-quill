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

        // ---------------------------
        // Reduce nested structures

        case Map(a, b, c) if (apply(a) != a || apply(c) != c) =>
          apply(Map(apply(a), b, apply(c)))

        case FlatMap(a, b, c) if (apply(a) != a || apply(c) != c) =>
          apply(FlatMap(apply(a), b, apply(c)))

        case Filter(a, b, c) if (apply(a) != a || apply(c) != c) =>
          apply(Filter(apply(a), b, apply(c)))

        case SortBy(a, b, c) if (apply(a) != a || apply(c) != c) =>
          apply(SortBy(apply(a), b, apply(c)))

        // ***************************
        // Symbolic reduction
        // ***************************

        // ---------------------------
        // map.*

        // a.map(b => c).map(d => e) =>
        //    a.map(b => e[d := c])
        case Map(Map(a, b, c), d, e) =>
          val er = BetaReduction(e, d -> c)
          apply(Map(a, b, er))

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

        // a.map(b => c).sortBy(d => e) =>
        //    a.sortBy(b => e[d := c]).map(b => c)
        case SortBy(Map(a, b, c), d, e) =>
          val er = BetaReduction(e, d -> c)
          apply(Map(SortBy(a, b, er), b, c))

        // ---------------------------
        // *.flatMap

        // a.filter(b => c).flatMap(d => e.$) =>
        //     a.flatMap(d => e.filter(_ => c[b := d]).$)
        case FlatMap(Filter(a, b, c), d, e: Query) =>
          val cr = BetaReduction(c, b -> d)
          val er = AttachToEntity(Filter(_, _, cr))(e)
          apply(FlatMap(a, d, er))

        // a.sortBy(b => c).flatMap(d => e.$) =>
        //     a.flatMap(d => e.sortBy(_ => c[b := d]).$)
        case FlatMap(SortBy(a, b, c), d, e: Query) =>
          val cr = BetaReduction(c, b -> d)
          val er = AttachToEntity(SortBy(_, _, cr))(e)
          apply(FlatMap(a, d, er))

        // a.flatMap(b => c).flatMap(d => e) =>
        //     a.flatMap(b => c.flatMap(d => e))
        case FlatMap(FlatMap(a, b, c), d, e) =>
          apply(FlatMap(a, b, FlatMap(c, d, e)))

        // ***************************
        // Ad-hoc reduction
        // ***************************

        // ---------------------------
        // sortBy.*

        // a.sortBy(b => c).filter(d => e) =>
        //     a.filter(d => e).sortBy(b => c)
        case Filter(SortBy(a, b, c), d, e) =>
          apply(SortBy(Filter(a, d, e), b, c))

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

        // ---------------------------
        // filter.filter

        // a.filter(b => c).filter(d => e) =>
        //    a.filter(b => c && e[d := b])
        case Filter(Filter(a, b, c), d, e) =>
          val er = BetaReduction(e, d -> b)
          apply(Filter(a, b, BinaryOperation(c, `&&`, er)))

        // ---------------------------
        // flatMap.*

        // a.flatMap(b => c).map(d => e) =>
        //    a.flatMap(b => c.map(d => e))
        case Map(FlatMap(a, b, c), d, e) =>
          apply(FlatMap(a, b, Map(c, d, e)))

        // a.flatMap(b => c).filter(d => e) =>
        //    a.flatMap(b => c.filter(d => e))
        case Filter(FlatMap(a, b, c), d, e) =>
          apply(FlatMap(a, b, Filter(c, d, e)))

        // a.flatMap(b => c).sortBy(d => e) =>
        //    a.flatMap(b => c.sortBy(d => e))
        case SortBy(FlatMap(a, b, c), d, e) =>
          apply(FlatMap(a, b, SortBy(c, d, e)))

        case other => other
      }
    }
}
