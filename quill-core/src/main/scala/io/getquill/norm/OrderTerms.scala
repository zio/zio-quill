package io.getquill.norm

import io.getquill.ast.Filter
import io.getquill.ast.GroupBy
import io.getquill.ast.Map
import io.getquill.ast.Query
import io.getquill.ast.SortBy
import io.getquill.ast.Take

object OrderTerms {

  def unapply(q: Query) =
    q match {

      case Take(Map(a: GroupBy, b, c), d) => None

      // a.sortBy(b => c).filter(d => e) =>
      //     a.filter(d => e).sortBy(b => c)
      case Filter(SortBy(a, b, c, d), e, f) =>
        Some(SortBy(Filter(a, e, f), b, c, d))

      // a.map(b => c).take(d) =>
      //    a.take(d).map(b => c)
      case Take(Map(a, b, c), d) =>
        Some(Map(Take(a, d), b, c))

      case other => None
    }
}
