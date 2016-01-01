package io.getquill.norm

import io.getquill.ast._
import io.getquill.util.Messages.fail

object AttachToEntity {

  def apply(f: (Query, Ident) => Query, alias: Option[Ident] = None)(q: Query): Query =
    q match {

      case Map(a: Entity, b, c)      => Map(f(a, b), b, c)
      case FlatMap(a: Entity, b, c)  => FlatMap(f(a, b), b, c)
      case Filter(a: Entity, b, c)   => Filter(f(a, b), b, c)
      case SortBy(a: Entity, b, c, d)   => SortBy(f(a, b), b, c, d)

      case Map(a: Query, b, c)       => Map(apply(f, Some(b))(a), b, c)
      case FlatMap(a: Query, b, c)   => FlatMap(apply(f, Some(b))(a), b, c)
      case Filter(a: Query, b, c)    => Filter(apply(f, Some(b))(a), b, c)
      case SortBy(a: Query, b, c, d)    => SortBy(apply(f, Some(b))(a), b, c, d)
      case Take(a: Query, b)         => Take(apply(f, alias)(a), b)
      case Drop(a: Query, b)         => Drop(apply(f, alias)(a), b)
      case GroupBy(a: Query, b, c)   => GroupBy(apply(f, Some(b))(a), b, c)
      case Aggregation(op, a: Query) => Aggregation(op, apply(f, alias)(a))

      case e: Entity                 => f(e, alias.getOrElse(Ident("x")))

      case other                     => fail(s"Can't find an 'Entity' in '$q'")
    }
}
