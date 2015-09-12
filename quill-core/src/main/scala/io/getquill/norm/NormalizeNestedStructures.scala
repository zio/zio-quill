package io.getquill.norm

import io.getquill.ast.Entity
import io.getquill.ast.Filter
import io.getquill.ast.FlatMap
import io.getquill.ast.Map
import io.getquill.ast.Query
import io.getquill.ast.SortBy
import io.getquill.ast.Reverse

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
      case Reverse(a) =>
        Normalize(a) match {
          case `a` => None
          case a   => Some(Reverse(a))
        }
    }
}
