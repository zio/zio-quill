package io.getquill.context.sql.norm

import io.getquill.ast._
import io.getquill.norm.BetaReduction
import io.getquill.norm.Normalize

object ExpandJoin {

  def apply(q: Ast) = expand(q, None)

  def expand(q: Ast, id: Option[Ident]) =
    Transform(q) {
      case q @ Join(_, _, _, Ident(a), Ident(b), _) =>
        val (qr, tuple) = expandedTuple(q)
        Map(qr, id.getOrElse(Ident(s"$a$b")), tuple)
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
        (Join(t, nestedExpand(a, tA), nestedExpand(b, tB), tA, tB, on), Tuple(List(tA, tB)))
    }

  private def nestedExpand(q: Ast, id: Ident) =
    Normalize(expand(q, Some(id))) match {
      case Map(q, _, _) => q
      case q            => q
    }
}