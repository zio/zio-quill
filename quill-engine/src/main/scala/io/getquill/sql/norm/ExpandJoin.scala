package io.getquill.context.sql.norm

import io.getquill.ast._
import io.getquill.ast.Implicits._
import io.getquill.norm.BetaReduction
import io.getquill.norm.TypeBehavior.{ReplaceWithReduction => RWR}
import io.getquill.norm.Normalize
import io.getquill.quat.Quat

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

class ExpandJoin(normalize: Normalize) {

  def apply(q: Ast) = expand(q, None)

  def expand(ast: Ast, id: Option[Ident]) =
    new StatelessTransformer {
      override def apply(q: Query): Query =
        q match {

          // Ident a and Ident b should have the same Quat, could add an assertion for that
          // WHAT??? THE SAME QUAT? WHY???
          case q @ Join(_, _, _, Ident(a, _), Ident(b, _), _) =>
            val (qr, tuple) = expandedTuple(q)
            val innermostOpt =
              CollectAst(qr) {
                case fm @ FlatMap(_, _, MoreTables) => fm
              }.headOption

            innermostOpt match {
              case Some(innermost) =>
                val newInnermost =
                  innermost match {
                    case FlatMap(fj: FlatJoin, alias, MoreTables) =>
                      // TODO reduce this out i.e. something && 1==1 should be just something
                      val fjr = BetaReduction(fj, RWR, MoreCond -> (Constant(1, Quat.Value) +==+ Constant(1, Quat.Value)))
                      Map(fjr, alias, tuple)
                    case other =>
                      throw new IllegalArgumentException(
                        s"Flat Join Expansion created Illegal FlatJoin COnstruct:\n${io.getquill.util.Messages.qprint(other).plainText}"
                      )
                  }
                val output: Query = BetaReduction(qr, RWR, innermost -> newInnermost).asInstanceOf[Query]

                // Check that there are no placeholders remaining in the AST. Otherwise something has gone wrong.
                val verifyNoPlaceholders = new StatelessTransformer {
                  override def applyIdent(id: Ident): Ident =
                    id match {
                      case `MoreCond` | `MoreTables` =>
                        throw new IllegalArgumentException(
                          s"Flat Join Expansion could not succeed due to placeholders remaining in the AST:\n${io.getquill.util.Messages.qprint(output).plainText}"
                        )
                      case _ =>
                        id
                    }
                }
                verifyNoPlaceholders(output)
                output

              case None =>
                qr
            }

          case _ => super.apply(q).asInstanceOf[Query]
        }
    }.apply(ast)

  val MoreTables = Ident("<MORE_TABLES>", Quat.Generic)
  val MoreCond   = Ident("<MORE_COND>", Quat.Generic)

  private def expandedTuple(q: Join): (FlatMap, Tuple) =
    q match {

      case Join(t, a: Join, b: Join, tA, tB, o) =>
        val (ar, at) = expandedTuple(a)
        val (br, bt) = expandedTuple(b)
        val or       = BetaReduction(o, RWR, tA -> at, tB -> bt)
        val arbrFlat = BetaReduction(ar, RWR, MoreTables -> br)
        val arbr =
          BetaReduction(arbrFlat, RWR, MoreCond -> or).asInstanceOf[FlatMap] // Reduction of a flatMap must be a flatMap
        (arbr, Tuple(List(at, bt)))

      case Join(t, a: Join, b, tA, tB, o) =>
        val (ar, at) = expandedTuple(a)
        val or       = BetaReduction(o, RWR, tA -> at)
        val br =
          FlatMap(
            FlatJoin(t, b, tB, or +&&+ MoreCond),
            tB,
            MoreTables
          )
        val arbr = BetaReduction(ar, RWR, MoreTables -> br).asInstanceOf[FlatMap]
        (arbr, Tuple(List(at, tB)))

      case Join(t, a, b: Join, tA, tB, o) =>
        val (br, bt) = expandedTuple(b)
        val or       = BetaReduction(o, RWR, tB -> bt)
        val arbr =
          FlatMap(
            FlatJoin(t, a, tA, or),
            tA,
            BetaReduction(br, RWR, MoreCond -> or)
          )

        (arbr, Tuple(List(tA, bt)))

      case q @ Join(t, a, b, tA, tB, on) =>
        // (Join(t, nestedExpand(a, tA), nestedExpand(b, tB), tA, tB, on), Tuple(List(tA, tB)))
        val ar = nestedExpand(a, tA)
        val br = nestedExpand(b, tB)
        val ab =
          FlatMap(
            ar,
            tA,
            FlatMap(FlatJoin(t, br, tB, on), tB, MoreTables)
          )

        (ab, Tuple(List(tA, tB)))
    }

  private def nestedExpand(q: Ast, id: Ident) =
    normalize(expand(q, Some(id))) match {
      case Map(q, _, _) => q
      case q            => q
    }
}
