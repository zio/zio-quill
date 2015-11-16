package io.getquill.norm.capture

import io.getquill.ast._
import io.getquill.norm.BetaReduction

private case class AvoidAliasConflict(state: Set[Ident])
    extends StatefulTransformer[Set[Ident]] {

  override def apply(q: Query): (Query, StatefulTransformer[Set[Ident]]) =
    q match {

      case FlatMap(q: Entity, x, p) =>
        apply(x, p)(FlatMap(q, _, _))

      case Map(q: Entity, x, p) =>
        apply(x, p)(Map(q, _, _))

      case Filter(q: Entity, x, p) =>
        apply(x, p)(Filter(q, _, _))

      case SortBy(q: Entity, x, p) =>
        apply(x, p)(SortBy(q, _, _))

      case OuterJoin(t, a, b, iA, iB, o) =>
        val freshA = freshIdent(iA)
        val freshB = freshIdent(iB)
        val or = BetaReduction(o, iA -> freshA, iB -> freshB)
        val (orr, orrt) = AvoidAliasConflict(state + freshA + freshB)(or)
        (OuterJoin(t, a, b, freshA, freshB, orr), orrt)

      case other => super.apply(other)
    }

  private def apply(x: Ident, p: Ast)(f: (Ident, Ast) => Query): (Query, StatefulTransformer[Set[Ident]]) = {
    val fresh = freshIdent(x)
    val pr = BetaReduction(p, x -> fresh)
    val (prr, t) = AvoidAliasConflict(state + fresh)(pr)
    (f(fresh, prr), t)
  }

  private def freshIdent(x: Ident): Ident =
    if (!state.contains(x))
      x
    else
      freshIdent(x, 1)

  private def freshIdent(x: Ident, n: Int): Ident = {
    val fresh = Ident(s"${x.name}$n")
    if (!state.contains(fresh))
      fresh
    else
      freshIdent(x, n + 1)
  }

}

private[capture] object AvoidAliasConflict {

  def apply(q: Query): Query =
    AvoidAliasConflict(Set[Ident]())(q) match {
      case (q, _) => q
    }
}
