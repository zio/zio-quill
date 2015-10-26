package io.getquill.source.sql

import io.getquill.ast._

object ExpandOuterJoin extends StatelessTransformer {

  override def apply(q: Query) =
    q match {
      case OuterJoin(t, a, b, tA, tB, on) =>
        Map(OuterJoin(t, apply(a), apply(b), tA, tB, on), Ident("temp"), Tuple(List(tA, tB)))
      case other =>
        super.apply(other)
    }
}
