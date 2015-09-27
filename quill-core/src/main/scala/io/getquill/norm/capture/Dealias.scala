package io.getquill.norm.capture

import io.getquill.ast._
import io.getquill.norm.BetaReduction

case class Dealias(state: Option[Ident]) extends StatefulTransformer[Option[Ident]] {

  override def apply(q: Query): (Query, StatefulTransformer[Option[Ident]]) =
    q match {
      case FlatMap(a, b, c) =>
        dealias(q, a, b, c)(FlatMap) match {
          case (FlatMap(a, b, c), _) =>
            val (cn, cnt) = apply(c)
            (FlatMap(a, b, cn), cnt)
        }
      case Map(a, b, c) =>
        dealias(q, a, b, c)(Map)
      case Filter(a, b, c) =>
        dealias(q, a, b, c)(Filter)
      case SortBy(a, b, c) =>
        dealias(q, a, b, c)(SortBy)
      case Reverse(a) =>
        val (an, ant) = apply(a)
        (Reverse(an), ant)
      case Take(a, b) =>
        val (an, ant) = apply(a)
        (Take(an, b), ant)
      case Drop(a, b) =>
        val (an, ant) = apply(a)
        (Drop(an, b), ant)
      case Union(a, b) =>
        val (an, ant) = apply(a)
        val (bn, bnt) = ant.apply(b)
        (Union(an, bn), bnt)
      case q: Entity =>
        (q, Dealias(None))
    }

  private def dealias[T](q: Query, a: Ast, b: Ident, c: Ast)(f: (Ast, Ident, Ast) => T) =
    apply(a) match {
      case (an, t @ Dealias(Some(alias))) =>
        (f(an, alias, BetaReduction(c, b -> alias)), t)
      case other =>
        (f(a, b, c), Dealias(Some(b)))
    }
}

object Dealias {
  def apply(query: Query) =
    new Dealias(None)(query) match {
      case (q, _) => q
    }
}
