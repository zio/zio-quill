package io.getquill.norm

import io.getquill.ast._

case class ReduceNestedStructures(apply: Ast => Ast) {

  def unapply(q: Query): Option[Query] =
    q match {
      case Map(a, b, c) =>
        (apply(a), apply(c)) match {
          case (`a`, `c`) => None
          case (a, c)     => Some(Map(a, b, c))
        }
      case FlatMap(a, b, c) =>
        (apply(a), apply(c)) match {
          case (`a`, `c`) => None
          case (a, c)     => Some(FlatMap(a, b, c))
        }
      case Filter(a, b, c) =>
        (apply(a), apply(c)) match {
          case (`a`, `c`) => None
          case (a, c)     => Some(Filter(a, b, c))
        }
      case SortBy(a, b, c) =>
        (apply(a), apply(c)) match {
          case (`a`, `c`) => None
          case (a, c)     => Some(SortBy(a, b, c))
        }
      case e: Entity => None
    }
}
