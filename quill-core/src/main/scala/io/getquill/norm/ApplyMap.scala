package io.getquill.norm

import io.getquill.ast._

object ApplyMap {

  private def isomorphic(e: Ast, c: Ast, alias: Ident) =
    BetaReduction(e, alias -> c) == c

  def unapply(q: Query): Option[Query] =
    q match {

      case Map(Map(a: GroupBy, b, c), d, e)       => None
      case FlatMap(Map(a: GroupBy, b, c), d, e)   => None
      case Filter(Map(a: GroupBy, b, c), d, e)    => None
      case SortBy(Map(a: GroupBy, b, c), d, e, f) => None
      case Take(Map(a: GroupBy, b, c), d)         => None
      case Drop(Map(a: GroupBy, b, c), d)         => None
      case Map(a: GroupBy, b, c) if (b == c)      => None
      case Map(a: Nested, b, c) if (b == c)       => None

      //  map(i => (i.i, i.l)).distinct.map(x => (x._1, x._2)) =>
      //    map(i => (i.i, i.l)).distinct
      case Map(Distinct(Map(a, b, c)), d, e) if isomorphic(e, c, d) =>
        Some(Distinct(Map(a, b, c)))

      // a.map(b => b) =>
      //    a
      case Map(a: Query, b, c) if (b == c) =>
        Some(a)

      // a.map(b => c).map(d => e) =>
      //    a.map(b => e[d := c])
      case Map(Map(a, b, c), d, e) =>
        val er = BetaReduction(e, d -> c)
        Some(Map(a, b, er))

      // a.map(b => c).flatMap(d => e) =>
      //    a.flatMap(b => e[d := c])
      case FlatMap(Map(a, b, c), d, e) =>
        val er = BetaReduction(e, d -> c)
        Some(FlatMap(a, b, er))

      // a.map(b => c).filter(d => e) =>
      //    a.filter(b => e[d := c]).map(b => c)
      case Filter(Map(a, b, c), d, e) =>
        val er = BetaReduction(e, d -> c)
        Some(Map(Filter(a, b, er), b, c))

      // a.map(b => c).sortBy(d => e) =>
      //    a.sortBy(b => e[d := c]).map(b => c)
      case SortBy(Map(a, b, c), d, e, f) =>
        val er = BetaReduction(e, d -> c)
        Some(Map(SortBy(a, b, er, f), b, c))

      // a.map(b => c).drop(d) =>
      //    a.drop(d).map(b => c)
      case Drop(Map(a, b, c), d) =>
        Some(Map(Drop(a, d), b, c))

      // a.map(b => c).take(d) =>
      //    a.drop(d).map(b => c)
      case Take(Map(a, b, c), d) =>
        Some(Map(Take(a, d), b, c))

      case other => None
    }
}