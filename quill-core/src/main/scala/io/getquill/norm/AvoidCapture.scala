package io.getquill.norm

import io.getquill.ast.Expr
import io.getquill.ast.Filter
import io.getquill.ast.FlatMap
import io.getquill.ast.Ident
import io.getquill.ast.Map
import io.getquill.ast.Query
import io.getquill.ast.Table

object AvoidCapture {

  def apply(q: Query): Query =
    dealias(reduction(q, Set())._1)

  private def dealias(q: Query): Query =
    q match {

      case FlatMap(Filter(q, x, p), y, r) =>
        val rr = BetaReduction(r, y -> x)
        FlatMap(dealias(Filter(q, x, p)), x, dealias(rr))

      case Filter(Filter(q, x, p), y, r) =>
        val rr = BetaReduction(r, y -> x)
        Filter(dealias(Filter(q, x, p)), x, rr)

      case Map(Filter(q, x, p), y, r) =>
        val rr = BetaReduction(r, y -> x)
        Map(dealias(Filter(q, x, p)), x, rr)

      // Recursion
      case FlatMap(q, x, p) => FlatMap(dealias(q), x, dealias(p))
      case Map(q, x, p)     => Map(dealias(q), x, p)
      case Filter(q, x, p)  => Filter(dealias(q), x, p)
      case t: Table         => t
    }

  private def reduction(q: Query, seen: Set[Ident]): (Query, Set[Ident]) = {
    q match {
      // Reduction
      case FlatMap(q: Table, x, p) =>
        val (xr, pr, rseen) = reduction(x, p, seen)
        val (prr, prrseen) = reduction(pr, rseen)
        (FlatMap(q, xr, prr), prrseen)
      case Map(q: Table, x, p) =>
        val (xr, pr, rseen) = reduction(x, p, seen)
        (Map(q, xr, pr), rseen)
      case Filter(q: Table, x, p) =>
        val (xr, pr, rseen) = reduction(x, p, seen)
        (Filter(q, xr, pr), rseen)

      // Recursion
      case FlatMap(q, x, p) =>
        val (qr, qrseen) = reduction(q, seen)
        val (pr, prseen) = reduction(p, qrseen)
        (FlatMap(qr, x, pr), prseen)
      case Map(q, x, p) =>
        val (qr, qrseen) = reduction(q, seen)
        (Map(qr, x, p), qrseen)
      case Filter(q, x, p) =>
        val (qr, qrseen) = reduction(q, seen)
        (Filter(qr, x, p), qrseen)
      case t: Table =>
        (t, Set())
    }
  }

  private def reduction(x: Ident, p: Query, seen: Set[Ident]): (Ident, Query, Set[Ident]) =
    reduction[Query](x, p, seen, BetaReduction.apply(_, _))

  private def reduction(x: Ident, p: Expr, seen: Set[Ident]): (Ident, Expr, Set[Ident]) =
    reduction[Expr](x, p, seen, BetaReduction.apply(_, _))

  private def reduction[T](x: Ident, p: T, seen: Set[Ident], betaReduction: (T, (Ident, Expr)) => T): (Ident, T, Set[Ident]) =
    if (seen.contains(x)) {
      val xr = freshIdent(x, seen)
      val pr = betaReduction(p, x -> xr)
      (xr, pr, seen + xr)
    }
    else
      (x, p, seen + x)

  private def freshIdent(x: Ident, seen: Set[Ident], n: Int = 1): Ident = {
    val fresh = Ident(s"${x.name}$n")
    if (!seen.contains(fresh))
      fresh
    else
      freshIdent(x, seen, n + 1)
  }
}