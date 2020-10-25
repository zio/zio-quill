package io.getquill.norm

import io.getquill.util.Messages.fail
import io.getquill.ast._

/**
 * Find the innermost clause that contains an entity and attach some other kind of clause into it, e.g. a Filter.
 * For example if you have an AST that looks like this:
 * {{{
 *   FlatMap(Map(Entity(A), a, B), b, C)
 * }}}
 * Then `AttachToEntity(Filter(_, _, Cond)` will result in this:
 * {{{
 *   FlatMap(Map(Filter(Entity(A), {tmp}, Cond), a, B), b, C)
 * }}}
 *
 * Note how the inner ident `{tmp}` needs to be unique and not conflict with any ident
 * higher in the AST that is used inside the Cond clause, otherwise, the
 * various kinds of ASTs will be irreversibly corrupted.
 * Here is an example:
 *
 * Take:
 * {{{
 *   FlatMap(A, a, Entity(C))
 * }}}
 *
 * Attached to the clause:
 * {{{
 *   Filter(_, {dangerous_tmp}, If(a == x, foo, bar))
 * }}}
 *
 * Which results in:
 * {{{
 *   FlatMap(A, a, Filter(Entity(C), {dangerous_tmp}, If(a == x, foo, bar))
 * }}}
 *
 * If `{dangerious_tmp}` is the Ident 'a' then the following happens:
 * (I have added curly braces {} around this Ident just to distinguish it)
 * {{{
 *   FlatMap(A, a, Filter(Entity(C), {a}, If(b == x, foo, bar))
 * }}}
 * At that point the 'a' inside the attached Filter and the outside FlatMap are indistinguishable
 * and indeed, the next phase of `AvoidAliasConflict` will likely turn this expression
 * into the following:
 * {{{
 *   FlatMap(A, a, Filter(Entity(C), {a1}, If(a1 == x, foo, bar))
 * }}}
 * This is of course completely incorrect because the ident {a1} should actually be {a}
 * referring to the ident of the outer FlatMap.
 */
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

      case Map(_: GroupBy, _, _) | _: Union | _: UnionAll | _: Join | _: FlatJoin => f(q, alias.getOrElse(Ident("x", q.quat)))

      case Map(a: Query, b, c) => Map(apply(f, Some(b))(a), b, c)
      case FlatMap(a: Query, b, c) => FlatMap(apply(f, Some(b))(a), b, c)
      case ConcatMap(a: Query, b, c) => ConcatMap(apply(f, Some(b))(a), b, c)
      case Filter(a: Query, b, c) => Filter(apply(f, Some(b))(a), b, c)
      case SortBy(a: Query, b, c, d) => SortBy(apply(f, Some(b))(a), b, c, d)
      case Take(a: Query, b) => Take(apply(f, alias)(a), b)
      case Drop(a: Query, b) => Drop(apply(f, alias)(a), b)
      case Aggregation(op, a: Query) => Aggregation(op, apply(f, alias)(a))
      case Distinct(a: Query) => Distinct(apply(f, alias)(a))

      case IsEntity(q) => f(q, alias.getOrElse(Ident(s"[tmp_${java.util.UUID.randomUUID().toString.replace("-", "")}]", q.quat)))
      case other => fail(s"Can't find an 'Entity' in '$q'")
    }
}
