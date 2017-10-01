package io.getquill.context.sql.norm

import io.getquill.ast.Ast
import io.getquill.ast.Filter
import io.getquill.ast.Ident
import io.getquill.ast.Join
import io.getquill.ast.Map
import io.getquill.ast.Query
import io.getquill.ast.StatelessTransformer
import io.getquill.ast.Tuple
import io.getquill.norm.BetaReduction

object ExpandJoin extends StatelessTransformer {

  override def apply(q: Query) =
    q match {
      case Filter(Expand(ar, at), b, c) =>
        val id = ident(at)
        val cr = BetaReduction(c, b -> at)
        Map(Filter(ar, id, cr), id, at)
      case Expand(qr, map) =>
        Map(qr, ident(map), map)
      case other => super.apply(other)
    }

  object Expand {
    def unapply(q: Ast): Option[(Ast, Ast)] =
      q match {
        case Join(t, Expand(ar, at), Expand(br, bt), tA, tB, o) =>
          val or = BetaReduction(o, tA -> at, tB -> bt)
          Some((Join(t, ar, br, tA, tB, or), Tuple(List(at, bt))))

        case Join(t, Expand(ar, at), b, tA, tB, o) =>
          val or = BetaReduction(o, tA -> at)
          Some((Join(t, ar, b, tA, tB, or), Tuple(List(at, tB))))

        case Join(t, a, Expand(br, bt), tA, tB, o) =>
          val or = BetaReduction(o, tB -> bt)
          Some((Join(t, a, br, tA, tB, or), Tuple(List(tA, bt))))

        case q @ Join(t, a, b, tA, tB, on) =>
          Some((q, Tuple(List(tA, tB))))

        case Filter(Expand(ar, at), b, c) =>
          val id = ident(at)
          val cr = BetaReduction(c, b -> at)
          Some((Filter(ar, id, cr), id))

        case _ => None
      }
  }

  private def ident(ast: Ast): Ident =
    ast match {
      case Tuple(values) =>
        values.map(ident).foldLeft(Ident("")) {
          case (Ident(a), Ident(b)) =>
            Ident(s"$a$b")
        }
      case i: Ident => i
      case other    => Ident(other.toString)
    }
}
