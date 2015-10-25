package io.getquill.norm.capture

import io.getquill.ast._
import io.getquill.norm.BetaReduction

private case class AvoidAliasConflict(state: Set[Ident])
    extends StatefulTransformer[Set[Ident]] {

  override def apply(q: Query): (Query, StatefulTransformer[Set[Ident]]) =
    q match {

      case FlatMap(q: Entity, x, p) if (state.contains(x)) =>
        val fresh = freshIdent(x)
        val pr = BetaReduction(p, x -> fresh)
        val (prr, t) = AvoidAliasConflict(state + fresh)(pr)
        (FlatMap(q, fresh, prr), t)

      case Map(q: Entity, x, p) if (state.contains(x)) =>
        val fresh = freshIdent(x)
        val pr = BetaReduction(p, x -> fresh)
        val (prr, t) = AvoidAliasConflict(state + fresh)(pr)
        (Map(q, fresh, prr), t)

      case Filter(q: Entity, x, p) if (state.contains(x)) =>
        val fresh = freshIdent(x)
        val pr = BetaReduction(p, x -> fresh)
        val (prr, t) = AvoidAliasConflict(state + fresh)(pr)
        (Filter(q, fresh, prr), t)

      case SortBy(q: Entity, x, p) if (state.contains(x)) =>
        val fresh = freshIdent(x)
        val pr = BetaReduction(p, x -> fresh)
        val (prr, t) = AvoidAliasConflict(state + fresh)(pr)
        (SortBy(q, fresh, prr), t)

      case OuterJoin(t, a, b, iA, iB, o) if (state.contains(iA) && state.contains(iB)) =>
        val freshA = freshIdent(iA)
        val freshB = freshIdent(iB)
        val or = BetaReduction(o, iA -> freshA, iB -> freshB)
        val (orr, orrt) = AvoidAliasConflict(state + freshA + freshB)(or)
        (OuterJoin(t, a, b, freshA, freshB, orr), orrt)

      case OuterJoin(t, a, b, iA, iB, o) if (state.contains(iA)) =>
        val fresh = freshIdent(iA)
        val or = BetaReduction(o, iA -> fresh)
        val (orr, orrt) = AvoidAliasConflict(state + fresh)(or)
        (OuterJoin(t, a, b, fresh, iB, orr), orrt)

      case OuterJoin(t, a, b, iA, iB, o) if (state.contains(iB)) =>
        val fresh = freshIdent(iB)
        val or = BetaReduction(o, iB -> fresh)
        val (orr, orrt) = AvoidAliasConflict(state + fresh)(or)
        (OuterJoin(t, a, b, iA, fresh, orr), orrt)

      case FlatMap(q: Entity, x, p) =>
        val (pr, t) = AvoidAliasConflict(state + x)(p)
        (FlatMap(q, x, pr), t)

      case Map(q: Entity, x, p) =>
        val (pr, t) = AvoidAliasConflict(state + x)(p)
        (Map(q, x, pr), t)

      case Filter(q: Entity, x, p) =>
        val (pr, t) = AvoidAliasConflict(state + x)(p)
        (Filter(q, x, pr), t)

      case SortBy(q: Entity, x, p) =>
        val (pr, t) = AvoidAliasConflict(state + x)(p)
        (SortBy(q, x, pr), t)

      case other => super.apply(other)
    }

  private def freshIdent(x: Ident, n: Int = 1): Ident = {
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
