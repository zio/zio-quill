package io.getquill.norm.capture

import io.getquill.ast.Entity
import io.getquill.ast.Filter
import io.getquill.ast.FlatMap
import io.getquill.ast.Ident
import io.getquill.ast.Map
import io.getquill.ast.Query
import io.getquill.ast.StatefulTransformer
import io.getquill.norm.BetaReduction

private[capture] case class AvoidAliasConflict(state: Set[Ident])
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

      case FlatMap(q: Entity, x, p) =>
        val (pr, t) = AvoidAliasConflict(state + x)(p)
        (FlatMap(q, x, pr), t)

      case Map(q: Entity, x, p) =>
        val (pr, t) = AvoidAliasConflict(state + x)(p)
        (Map(q, x, pr), t)

      case Filter(q: Entity, x, p) =>
        val (pr, t) = AvoidAliasConflict(state + x)(p)
        (Filter(q, x, pr), t)

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
