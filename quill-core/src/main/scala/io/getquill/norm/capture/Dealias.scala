package io.getquill.norm.capture

import io.getquill.ast._
import io.getquill.norm.BetaReduction

case class Dealias(state: Option[Ident]) extends StatefulTransformer[Option[Ident]] {

  override def apply(q: Query): (Query, StatefulTransformer[Option[Ident]]) =
    q match {
      case FlatMap(a, b, c) =>
        dealias(a, b, c)(FlatMap) match {
          case (FlatMap(a, b, c), _) =>
            val (cn, cnt) = apply(c)
            (FlatMap(a, b, cn), cnt)
        }
      case Map(a, b, c) =>
        dealias(a, b, c)(Map)
      case Filter(a, b, c) =>
        dealias(a, b, c)(Filter)
      case SortBy(a, b, c) =>
        dealias(a, b, c)(SortBy)
      case GroupBy(a, b, c) =>
        dealias(a, b, c)(GroupBy)
      case q: Aggregation =>
        (q, Dealias(None))
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
        val (an, _) = apply(a)
        val (bn, _) = apply(b)
        (Union(an, bn), Dealias(None))
      case UnionAll(a, b) =>
        val (an, _) = apply(a)
        val (bn, _) = apply(b)
        (UnionAll(an, bn), Dealias(None))
      case LeftJoin(a, b) =>
        val (an, _) = apply(a)
        val (bn, _) = apply(b)
        (LeftJoin(an, bn), Dealias(None))
      case RightJoin(a, b) =>
        val (an, _) = apply(a)
        val (bn, _) = apply(b)
        (RightJoin(an, bn), Dealias(None))
      case FullJoin(a, b) =>
        val (an, _) = apply(a)
        val (bn, _) = apply(b)
        (FullJoin(an, bn), Dealias(None))
      case ConditionalOuterJoin(LeftJoin(a, b), c, d, e) =>
        dealias(a, b, c, d, e)(LeftJoin)
      case ConditionalOuterJoin(RightJoin(a, b), c, d, e) =>
        dealias(a, b, c, d, e)(RightJoin)
      case ConditionalOuterJoin(FullJoin(a, b), c, d, e) =>
        dealias(a, b, c, d, e)(FullJoin)
      case _: Entity | _: ConditionalOuterJoin =>
        (q, Dealias(None))
    }

  private def dealias(
    a: Ast, b: Ast, c: Ident, d: Ident, e: Ast)(
      f: (Ast, Ast) => OuterJoin): (ConditionalOuterJoin, Dealias) = {
    val ((an, cn, en), ent) = dealias(a, c, e)((_, _, _))
    val ((bn, dn, enn), ennt) = ent.dealias(b, d, en)((_, _, _))
    (ConditionalOuterJoin(f(an, bn), cn, dn, enn), Dealias(None))
  }

  private def dealias[T](a: Ast, b: Ident, c: Ast)(f: (Ast, Ident, Ast) => T) =
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
