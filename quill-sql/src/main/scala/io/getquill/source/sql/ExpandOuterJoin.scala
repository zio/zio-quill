package io.getquill.source.sql

import io.getquill.ast._

object ExpandOuterJoin extends StatelessTransformer {

  override def apply(q: Query) =
    q match {
      case q @ OuterJoin(t, a, b, tA, tB, on) =>
        Map(OuterJoin(t, apply(a), apply(b), tA, tB, on), Ident("temp"), expandedTuple(q))
      case other =>
        super.apply(other)
    }

  private def expandedTuple(q: OuterJoin): Tuple =
    q match {
      case OuterJoin(t, a: OuterJoin, b: OuterJoin, tA, tB, on) =>
        Tuple(List(expandedTuple(a), expandedTuple(b)))
      case OuterJoin(t, a: OuterJoin, b, tA, tB, on) =>
        Tuple(List(expandedTuple(a), tB))
      case OuterJoin(t, a, b: OuterJoin, tA, tB, on) =>
        Tuple(List(tA, expandedTuple(b)))
      case OuterJoin(t, a, b, tA, tB, on) =>
        Tuple(List(tA, tB))
    }
}
