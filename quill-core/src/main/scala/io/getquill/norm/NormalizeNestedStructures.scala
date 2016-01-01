package io.getquill.norm

import io.getquill.ast._

object NormalizeNestedStructures {

  def unapply(q: Query): Option[Query] =
    q match {
      case e: Entity         => None
      case Map(a, b, c)      => apply(a, c)(Map(_, b, _))
      case FlatMap(a, b, c)  => apply(a, c)(FlatMap(_, b, _))
      case Filter(a, b, c)   => apply(a, c)(Filter(_, b, _))
      case SortBy(a, b, c, d)   => apply(a, c)(SortBy(_, b, _, d))
      case GroupBy(a, b, c)  => apply(a, c)(GroupBy(_, b, _))
      case Aggregation(a, b) => apply(b)(Aggregation(a, _))
      case Take(a, b)        => apply(a, b)(Take)
      case Drop(a, b)        => apply(a, b)(Drop)
      case Union(a, b)       => apply(a, b)(Union)
      case UnionAll(a, b)    => apply(a, b)(UnionAll)
      case OuterJoin(t, a, b, iA, iB, on) =>
        (Normalize(a), Normalize(b), Normalize(on)) match {
          case (`a`, `b`, `on`) => None
          case (a, b, on)       => Some(OuterJoin(t, a, b, iA, iB, on))
        }
    }

  private def apply(a: Ast)(f: Ast => Query) =
    (Normalize(a)) match {
      case (`a`) => None
      case (a)   => Some(f(a))
    }

  private def apply(a: Ast, b: Ast)(f: (Ast, Ast) => Query) =
    (Normalize(a), Normalize(b)) match {
      case (`a`, `b`) => None
      case (a, b)     => Some(f(a, b))
    }
}
