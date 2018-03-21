package io.getquill.norm

import io.getquill.ast._

object NormalizeAggregationIdent {

  def unapply(q: Query) =
    q match {

      // a => a.b.map(x => x.c).agg =>
      //   a => a.b.map(a => a.c).agg
      case Aggregation(op, Map(p @ Property(i: Ident, _), mi, Property(_: Ident, n))) if i != mi =>
        Some(Aggregation(op, Map(p, i, Property(i, n))))

      case _ => None
    }
}
