package io.getquill.norm

import io.getquill.ast._

object NormalizeAggregationIdent {

  def unapply(q: Query) =
    q match {

      // a => a.b.map(x => x.c).agg =>
      //   a => a.b.map(a => a.c).agg
      case Aggregation(op, Map(p @ Property(i: Ident, _), mi, Property.Opinionated(_: Ident, n, renameable, visibility))) if i != mi =>
        Some(Aggregation(op, Map(p, i, Property.Opinionated(i, n, renameable, visibility)))) // in example aove, if c in x.c is fixed c in a.c should also be

      case _ => None
    }
}
