package io.getquill.norm

import io.getquill.ast._

object NormalizeNestedStructures {

  def unapply(q: Query): Option[Query] =
    q match {
      case e: Entity => None
      case Map(a, b, c) =>
        (Normalize(a), Normalize(c)) match {
          case (`a`, `c`) => None
          case (a, c)     => Some(Map(a, b, c))
        }
      case FlatMap(a, b, c) =>
        (Normalize(a), Normalize(c)) match {
          case (`a`, `c`) => None
          case (a, c)     => Some(FlatMap(a, b, c))
        }
      case Filter(a, b, c) =>
        (Normalize(a), Normalize(c)) match {
          case (`a`, `c`) => None
          case (a, c)     => Some(Filter(a, b, c))
        }
      case SortBy(a, b, c) =>
        (Normalize(a), Normalize(c)) match {
          case (`a`, `c`) => None
          case (a, c)     => Some(SortBy(a, b, c))
        }
      case GroupBy(a, b, c) =>
        (Normalize(a), Normalize(c)) match {
          case (`a`, `c`) => None
          case (a, c)     => Some(GroupBy(a, b, c))
        }
      case Aggregation(a, b) =>
        Normalize(b) match {
          case `b` => None
          case b   => Some(Aggregation(a, b))
        }
      case Reverse(a) =>
        Normalize(a) match {
          case `a` => None
          case a   => Some(Reverse(a))
        }
      case Take(a, b) =>
        (Normalize(a), Normalize(b)) match {
          case (`a`, `b`) => None
          case (a, b)     => Some(Take(a, b))
        }
      case Drop(a, b) =>
        (Normalize(a), Normalize(b)) match {
          case (`a`, `b`) => None
          case (a, b)     => Some(Drop(a, b))
        }
      case Union(a, b) =>
        (Normalize(a), Normalize(b)) match {
          case (`a`, `b`) => None
          case (a, b)     => Some(Union(a, b))
        }
      case UnionAll(a, b) =>
        (Normalize(a), Normalize(b)) match {
          case (`a`, `b`) => None
          case (a, b)     => Some(UnionAll(a, b))
        }
      case e: OuterJoin =>
        unapply(q)
    }

  private def unapply(q: OuterJoin): Option[OuterJoin] =
    q match {
      case LeftJoin(a, b) =>
        (Normalize(a), Normalize(b)) match {
          case (`a`, `b`) => None
          case (a, b)     => Some(LeftJoin(a, b))
        }
      case RightJoin(a, b) =>
        (Normalize(a), Normalize(b)) match {
          case (`a`, `b`) => None
          case (a, b)     => Some(RightJoin(a, b))
        }
      case FullJoin(a, b) =>
        (Normalize(a), Normalize(b)) match {
          case (`a`, `b`) => None
          case (a, b)     => Some(FullJoin(a, b))
        }
      case ConditionalOuterJoin(a, b, c, d) =>
        (Normalize(a), Normalize(d)) match {
          case (`a`, `d`) => None
          case (a, d)     => Some(ConditionalOuterJoin(a, b, c, d))
        }
    }
}
