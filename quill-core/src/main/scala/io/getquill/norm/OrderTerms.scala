package io.getquill.norm

import io.getquill.ast._

object OrderTerms {

  def unapply(q: Query) =
    q match {

      // a.reverse.filter(b => c) =>
      //     a.filter(b => c).reverse
      case Filter(Reverse(a), b, c) =>
        Some(Reverse(Filter(a, b, c)))

      // a.map(b => c).reverse =>
      //     a.reverse.map(b => c)
      case Reverse(Map(a, b, c)) =>
        Some(Map(Reverse(a), b, c))

      // a.sortBy(b => c).filter(d => e) =>
      //     a.filter(d => e).sortBy(b => c)
      case Filter(SortBy(a, b, c), d, e) =>
        Some(SortBy(Filter(a, d, e), b, c))

      // a.map(b => c).take(d) =>
      //    a.take(d).map(b => c)
      case Take(Map(a, b, c), d) =>
        Some(Map(Take(a, d), b, c))

      case other => None
    }
}
