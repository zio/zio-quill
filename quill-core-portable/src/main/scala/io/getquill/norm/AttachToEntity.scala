package io.getquill.norm

import io.getquill.util.Messages.fail
import io.getquill.ast._

object AttachToEntity {

  private object IsEntity {
    def unapply(q: Ast): Option[Ast] =
      q match {
        case q: Entity => Some(q)
        case q: Infix  => Some(q)
        case _         => None
      }
  }

  def apply(f: (Ast, Ident) => Query, alias: Option[Ident] = None)(q: Ast): Ast =
    q match {

      case Map(IsEntity(a), b, c) => Map(f(a, b), b, c)
      case FlatMap(IsEntity(a), b, c) => FlatMap(f(a, b), b, c)
      case ConcatMap(IsEntity(a), b, c) => ConcatMap(f(a, b), b, c)
      case Filter(IsEntity(a), b, c) => Filter(f(a, b), b, c)
      case SortBy(IsEntity(a), b, c, d) => SortBy(f(a, b), b, c, d)

      case Map(_: GroupBy, _, _) | _: Union | _: UnionAll | _: Join | _: FlatJoin => f(q, alias.getOrElse(Ident("x")))

      case Map(a: Query, b, c) => Map(apply(f, Some(b))(a), b, c)
      case FlatMap(a: Query, b, c) => FlatMap(apply(f, Some(b))(a), b, c)
      case ConcatMap(a: Query, b, c) => ConcatMap(apply(f, Some(b))(a), b, c)
      case Filter(a: Query, b, c) => Filter(apply(f, Some(b))(a), b, c)
      case SortBy(a: Query, b, c, d) => SortBy(apply(f, Some(b))(a), b, c, d)
      case Take(a: Query, b) => Take(apply(f, alias)(a), b)
      case Drop(a: Query, b) => Drop(apply(f, alias)(a), b)
      case Aggregation(op, a: Query) => Aggregation(op, apply(f, alias)(a))
      case Distinct(a: Query) => Distinct(apply(f, alias)(a))

      case IsEntity(q) => f(q, alias.getOrElse(Ident("x")))

      case other => fail(s"Can't find an 'Entity' in '$q'")
    }
}
