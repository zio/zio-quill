package io.getquill.norm

import io.getquill.ast.Entity
import io.getquill.ast.Filter
import io.getquill.ast.FlatMap
import io.getquill.ast.Ident
import io.getquill.ast.Map
import io.getquill.ast.Query
import io.getquill.ast.SortBy

case class AttachToEntity(f: (Query, Ident) => Query) {

  def apply(q: Query): Query =
    q match {

      case Map(a: Entity, b, c)     => Map(f(a, b), b, c)
      case FlatMap(a: Entity, b, c) => FlatMap(f(a, b), b, c)
      case Filter(a: Entity, b, c)  => Filter(f(a, b), b, c)
      case SortBy(a: Entity, b, c)  => SortBy(f(a, b), b, c)

      case Map(a: Query, b, c)      => Map(apply(a), b, c)
      case FlatMap(a: Query, b, c)  => FlatMap(apply(a), b, c)
      case Filter(a: Query, b, c)   => Filter(apply(a), b, c)
      case SortBy(a: Query, b, c)   => SortBy(apply(a), b, c)

      case e: Entity                => f(e, Ident("x"))

      case other                    => other
    }
}
