package io.getquill.norm

import io.getquill.ast._

object ApplyIntermediateMap {

  def unapply(q: Query): Option[Query] =
    q match {

      case Map(Map(a: GroupBy, b, c), d, e)     => None
      case FlatMap(Map(a: GroupBy, b, c), d, e) => None
      case Filter(Map(a: GroupBy, b, c), d, e)  => None
      case SortBy(Map(a: GroupBy, b, c), d, e)  => None

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
      case SortBy(Map(a, b, c), d, e) =>
        val er = BetaReduction(e, d -> c)
        Some(Map(SortBy(a, b, er), b, c))

      // a.map(b => c).outerJoin(d).on((iA, iB) => f) =>
      //    a.outerJoin(d).on((b, iB) => f[iA := c]).map(b => c)
      case OuterJoin(t, Map(a, b, c), d, iA, iB, f) =>
        val fr = BetaReduction(f, iA -> c)
        Some(Map(OuterJoin(t, a, d, b, iB, fr), b, c))

      // a.outerJoin(b.map(c => d)).on((iA, iB) => f) =>
      //    a.outerJoin(b).on((iA, c) => f[iB := d]).map(c => d)
      case OuterJoin(t, a, Map(b, c, d), iA, iB, f) =>
        val fr = BetaReduction(f, iB -> d)
        Some(Map(OuterJoin(t, a, b, iA, c, fr), c, d))

      case other => None
    }
}
