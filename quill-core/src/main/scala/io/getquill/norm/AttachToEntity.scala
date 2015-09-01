package io.getquill.norm

import io.getquill.util.Messages._
import io.getquill.ast.Entity
import io.getquill.ast.Filter
import io.getquill.ast.FlatMap
import io.getquill.ast.Ident
import io.getquill.ast.Map
import io.getquill.ast.Query
import io.getquill.ast.SortBy
import io.getquill.ast.Reverse

object AttachToEntity {

  def apply(f: (Query, Ident) => Query)(q: Query): Query =
    q match {

      case Map(a: Entity, b, c)     => Map(f(a, b), b, c)
      case FlatMap(a: Entity, b, c) => FlatMap(f(a, b), b, c)
      case Filter(a: Entity, b, c)  => Filter(f(a, b), b, c)
      case SortBy(a: Entity, b, c)  => SortBy(f(a, b), b, c)

      case Map(a: Query, b, c)      => Map(apply(f)(a), b, c)
      case FlatMap(a: Query, b, c)  => FlatMap(apply(f)(a), b, c)
      case Filter(a: Query, b, c)   => Filter(apply(f)(a), b, c)
      case SortBy(a: Query, b, c)   => SortBy(apply(f)(a), b, c)
      case Reverse(a: Query)        => Reverse(apply(f)(a))

      case e: Entity                => f(e, Ident("x"))

      case other                    => fail(s"Can't find an 'Entity' in '$q'")
    }
}
