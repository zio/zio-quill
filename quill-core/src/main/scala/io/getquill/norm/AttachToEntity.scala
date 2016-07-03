package io.getquill.norm

import io.getquill.ast.Aggregation
import io.getquill.ast.Distinct
import io.getquill.ast.Drop
import io.getquill.ast.Entity
import io.getquill.ast.Filter
import io.getquill.ast.FlatMap
import io.getquill.ast.GroupBy
import io.getquill.ast.Ident
import io.getquill.ast.Join
import io.getquill.ast.Map
import io.getquill.ast.Query
import io.getquill.ast.SortBy
import io.getquill.ast.Take
import io.getquill.ast.Union
import io.getquill.ast.UnionAll
import io.getquill.util.Messages.fail

object AttachToEntity {

  def apply(f: (Query, Ident) => Query, alias: Option[Ident] = None)(q: Query): Query =
    q match {

      case Map(a: Entity, b, c)             => Map(f(a, b), b, c)
      case FlatMap(a: Entity, b, c)         => FlatMap(f(a, b), b, c)
      case Filter(a: Entity, b, c)          => Filter(f(a, b), b, c)
      case SortBy(a: Entity, b, c, d)       => SortBy(f(a, b), b, c, d)

      case Map(a: Query, b, c)              => Map(apply(f, Some(b))(a), b, c)
      case FlatMap(a: Query, b, c)          => FlatMap(apply(f, Some(b))(a), b, c)
      case Filter(a: Query, b, c)           => Filter(apply(f, Some(b))(a), b, c)
      case SortBy(a: Query, b, c, d)        => SortBy(apply(f, Some(b))(a), b, c, d)
      case Take(a: Query, b)                => Take(apply(f, alias)(a), b)
      case Drop(a: Query, b)                => Drop(apply(f, alias)(a), b)
      case GroupBy(a: Query, b, c)          => GroupBy(apply(f, Some(b))(a), b, c)
      case Aggregation(op, a: Query)        => Aggregation(op, apply(f, alias)(a))
      case Distinct(a: Query)               => Distinct(apply(f, alias)(a))

      case _: Union | _: UnionAll | _: Join => f(q, alias.getOrElse(Ident("x")))

      case e: Entity                        => f(e, alias.getOrElse(Ident("x")))

      case other                            => fail(s"Can't find an 'Entity' in '$q'")
    }
}
