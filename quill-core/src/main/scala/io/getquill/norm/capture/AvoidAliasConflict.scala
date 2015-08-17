package io.getquill.norm.capture

import io.getquill.ast._
import io.getquill.norm.BetaReduction

private[capture] case class AvoidAliasConflict(seen: Set[Ident])
    extends Transformer {

  override def apply(q: Query): (Query, Transformer) = {
    println(q)
    q match {

      case FlatMap(q: Table, x, p) if (seen.contains(x)) =>
        val fresh = freshIdent(x, seen)
        val pr = BetaReduction(p, x -> fresh)
        val (prr, t) = AvoidAliasConflict(seen + fresh)(pr)
        (FlatMap(q, fresh, prr), t)

      case Map(q: Table, x, p) if (seen.contains(x)) =>
        val fresh = freshIdent(x, seen)
        val pr = BetaReduction(p, x -> fresh)
        val (prr, t) = AvoidAliasConflict(seen + fresh)(pr)
        (Map(q, fresh, prr), t)

      case Filter(q: Table, x, p) if (seen.contains(x)) =>
        val fresh = freshIdent(x, seen)
        val pr = BetaReduction(p, x -> fresh)
        val (prr, t) = AvoidAliasConflict(seen + fresh)(pr)
        (Filter(q, fresh, prr), t)

      case FlatMap(q: Table, x, p) =>
        val (pr, t) = AvoidAliasConflict(seen + x)(p)
        (FlatMap(q, x, pr), t)

      case Map(q: Table, x, p) =>
        val (pr, t) = AvoidAliasConflict(seen + x)(p)
        (Map(q, x, pr), t)

      case Filter(q: Table, x, p) =>
        val (pr, t) = AvoidAliasConflict(seen + x)(p)
        (Filter(q, x, pr), t)

      case other => super.apply(other)
    }
  }

  private def freshIdent(x: Ident, seen: Set[Ident], n: Int = 1): Ident = {
    val fresh = Ident(s"${x.name}$n")
    if (!seen.contains(fresh))
      fresh
    else
      freshIdent(x, seen, n + 1)
  }

}

private[capture] object AvoidAliasConflict {

  def apply(q: Query): Query = AvoidAliasConflict(Set[Ident]())(q)._1

}