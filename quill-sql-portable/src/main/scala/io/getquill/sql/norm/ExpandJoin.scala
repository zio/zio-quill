package io.getquill.context.sql.norm

import io.getquill.ast._
import io.getquill.norm.BetaReduction
import io.getquill.norm.Normalize

// TODO Quat comment here about the other branch where this is done
//      via flatmaps and technically this does not typecheck but left it alone
//      because inner aliases not expanded correctly from the following query
//
//    def nestedWithRenames(): Unit = {
//      case class Ent(name: String)
//      case class Foo(fame: String)
//      case class Bar(bame: String)
//
//      implicit val entSchema = schemaMeta[Ent]("TheEnt", _.name -> "theName")
//
//      val q = quote {
//        query[Foo]
//          .join(query[Ent]).on((f, e) => f.fame == e.name) // (Foo, Ent)
//          .distinct
//          .join(query[Bar]).on((fe, b) => (fe._1.fame == b.bame)) // ((Foo, Ent), Bar)
//          .distinct
//          .map(feb => (feb._1._2, feb._2)) // feb: ((Foo, Ent), Bar)
//          .distinct
//          .map(eb => (eb._1.name, eb._2.bame)) // eb: (Ent, Bar)
//      }
//      println(run(q)) //helloo
//    }
//    nestedWithRenames()

object ExpandJoin {

  def apply(q: Ast) = expand(q, None)

  def expand(q: Ast, id: Option[Ident]) =
    Transform(q) {
      case q @ Join(_, _, _, Ident(a, _), Ident(b, _), _) => // Ident a and Ident b should have the same Quat, could add an assertion for that
        val (qr, tuple) = expandedTuple(q)
        Map(qr, id.getOrElse(Ident(s"$a$b", q.quat)), tuple)
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