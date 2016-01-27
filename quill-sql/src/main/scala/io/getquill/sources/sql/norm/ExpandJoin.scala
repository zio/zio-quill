package io.getquill.sources.sql.norm

import io.getquill.ast._
import io.getquill.norm.BetaReduction

object ExpandJoin extends StatelessTransformer {

  override def apply(q: Query) =
    q match {
      case q @ Join(_, _, _, Ident(a), Ident(b), _) =>
        val (qr, tuple) = expandedTuple(q)
        Map(qr, Ident(s"$a$b"), tuple)
      case other => super.apply(other)
    }

  private def expandedTuple(q: Join): (Join, Tuple) =
    q match {

      case Join(t, a: Join, b: Join, tA, tB, o) =>
        val (ar, at) = expandedTuple(a)
        val (br, bt) = expandedTuple(b)
        val or = BetaReduction(o, tA -> at, tB -> bt)
        (Join(t, ar, br, tA, tB, or), Tuple(List(at, bt)))

      case Join(t, a: Join, b, tA, tB, o) =>
        val (ar, at) = expandedTuple(a)
        val or = BetaReduction(o, tA -> at)
        (Join(t, ar, b, tA, tB, or), Tuple(List(at, tB)))

      case Join(t, a, b: Join, tA, tB, o) =>
        val (br, bt) = expandedTuple(b)
        val or = BetaReduction(o, tB -> bt)
        (Join(t, a, br, tA, tB, or), Tuple(List(tA, bt)))

      case q @ Join(t, a, b, tA, tB, on) =>
        (q, Tuple(List(tA, tB)))
    }
}
