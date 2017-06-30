package io.getquill.norm.capture

import io.getquill.ast.Ast
import io.getquill.ast.Entity
import io.getquill.ast.Filter
import io.getquill.ast.FlatMap
import io.getquill.ast.Ident
import io.getquill.ast.Join
import io.getquill.ast.Map
import io.getquill.ast.Query
import io.getquill.ast.SortBy
import io.getquill.ast.StatefulTransformer
import io.getquill.norm.BetaReduction
import io.getquill.ast.FlatJoin

private case class AvoidAliasConflict(state: collection.Set[Ident])
  extends StatefulTransformer[collection.Set[Ident]] {

  override def apply(q: Query): (Query, StatefulTransformer[collection.Set[Ident]]) =
    q match {

      case FlatMap(q: Entity, x, p) =>
        apply(x, p)(FlatMap(q, _, _))

      case Map(q: Entity, x, p) =>
        apply(x, p)(Map(q, _, _))

      case Filter(q: Entity, x, p) =>
        apply(x, p)(Filter(q, _, _))

      case SortBy(q: Entity, x, p, o) =>
        apply(x, p)(SortBy(q, _, _, o))

      case Join(t, a, b, iA, iB, o) =>
        val (ar, art) = apply(a)
        val (br, brt) = art.apply(b)
        val freshA = freshIdent(iA, brt.state)
        val freshB = freshIdent(iB, brt.state)
        val or = BetaReduction(o, iA -> freshA, iB -> freshB)
        val (orr, orrt) = AvoidAliasConflict(brt.state + freshA + freshB)(or)
        (Join(t, ar, br, freshA, freshB, orr), orrt)

      case FlatJoin(t, a, iA, o) =>
        val (ar, art) = apply(a)
        val freshA = freshIdent(iA)
        val or = BetaReduction(o, iA -> freshA)
        val (orr, orrt) = AvoidAliasConflict(art.state + freshA)(or)
        (FlatJoin(t, ar, freshA, orr), orrt)

      case other => super.apply(other)
    }

  private def apply(x: Ident, p: Ast)(f: (Ident, Ast) => Query): (Query, StatefulTransformer[collection.Set[Ident]]) = {
    val fresh = freshIdent(x)
    val pr = BetaReduction(p, x -> fresh)
    val (prr, t) = AvoidAliasConflict(state + fresh)(pr)
    (f(fresh, prr), t)
  }

  private def freshIdent(x: Ident, state: collection.Set[Ident] = state): Ident =
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
    AvoidAliasConflict(collection.Set[Ident]())(q) match {
      case (q, _) => q
    }
}
