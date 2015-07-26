package io.getquill.norm

import io.getquill.ast._

object AvoidCapture {

  def apply(q: Query): Query =
    apply(q, Set())._1

  private def apply(q: Query, seen: Set[Ident]): (Query, Set[Ident]) = {
    q match {
      // Reduction
      case FlatMap(q: Table, x, p) =>
        val (xr, pr, rseen) = apply(x, p, seen)
        val (prr, prrseen) = apply(pr, rseen)
        (FlatMap(q, xr, prr), prrseen)
      case Map(q: Table, x, p) =>
        val (xr, pr, rseen) = apply(x, p, seen)
        (Map(q, xr, pr), rseen)
      case Filter(q: Table, x, p) =>
        val (xr, pr, rseen) = apply(x, p, seen)
        (Filter(q, xr, pr), rseen)

      // Recursion
      case FlatMap(q, x, p) =>
        val (qr, qrseen) = apply(q, seen)
        val (pr, prseen) = apply(p, qrseen)
        (FlatMap(qr, x, pr), prseen)
      case Map(q, x, p) =>
        val (qr, qrseen) = apply(q, seen)
        (Map(qr, x, p), qrseen)
      case Filter(q, x, p) =>
        val (qr, qrseen) = apply(q, seen)
        (Filter(qr, x, p), qrseen)
      case t: Table =>
        (t, Set())
    }
  }

  private def apply(x: Ident, p: Query, seen: Set[Ident]): (Ident, Query, Set[Ident]) =
    if (seen.contains(x)) {
      val xr = freshIdent(x, seen)
      val pr = BetaReduction(p)(collection.Map(x -> xr))
      (xr, pr, seen + xr)
    } else
      (x, p, seen + x)

  private def apply(x: Ident, p: Expr, seen: Set[Ident]): (Ident, Expr, Set[Ident]) =
    if (seen.contains(x)) {
      val xr = freshIdent(x, seen)
      val pr = BetaReduction(p)(collection.Map(x -> xr))
      (xr, pr, seen + xr)
    } else
      (x, p, seen + x)

  private def freshIdent(x: Ident, seen: Set[Ident]): Ident =
    if (seen.contains(x))
      freshIdent(Ident(x.name + x.name), seen)
    else
      x
}