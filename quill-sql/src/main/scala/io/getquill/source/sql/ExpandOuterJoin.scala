package io.getquill.source.sql

import io.getquill.ast._
import io.getquill.norm.BetaReduction

object ExpandOuterJoin extends StatelessTransformer {

  override def apply(q: Query) =
    q match {
      case q: OuterJoin =>
        val (qr, tuple) = expandedTuple(q)
        Map(qr, Ident("temp"), tuple)
      case other => super.apply(other)
    }

  private def expandedTuple(q: OuterJoin): (OuterJoin, Tuple) =
    q match {
      case OuterJoin(t, a: OuterJoin, b: OuterJoin, tA, tB, o) =>
        val (ar, at) = expandedTuple(a)
        val (br, bt) = expandedTuple(b)
        val or = BetaReduction(o, tA -> at, tB -> bt)
        (OuterJoin(t, ar, br, tA, tB, or), Tuple(List(at, bt)))
      case OuterJoin(t, a: OuterJoin, b, tA, tB, o) =>
        val (ar, at) = expandedTuple(a)
        val or = BetaReduction(o, tA -> at)
        (OuterJoin(t, ar, b, tA, tB, or), Tuple(List(at, tB)))
      case OuterJoin(t, a, b: OuterJoin, tA, tB, o) =>
        val (br, bt) = expandedTuple(b)
        val or = BetaReduction(o, tB -> bt)
        (OuterJoin(t, a, br, tA, tB, or), Tuple(List(tA, bt)))
      case q @ OuterJoin(t, a, b, tA, tB, on) =>
        (q, Tuple(List(tA, tB)))
    }
}
