package io.getquill.norm

import io.getquill.ast._
import io.getquill.quat.QuatNestingHelper._

/**
 * A problem occurred in the original way infixes were done in that it was assumed that infix
 * clauses represented pure functions. While this is true of many UDFs (e.g. `CONCAT`, `GETDATE`)
 * it is certainly not true of many others e.g. `RAND()`, and most importantly `RANK()`. For this reason,
 * the operations that are done in `ApplyMap` on standard AST `Map` clauses cannot be done therefore additional
 * safety checks were introduced there in order to assure this does not happen. In addition to this however,
 * it is necessary to add this normalization step which inserts `Nested` AST elements in every map
 * that contains impure infix. See more information and examples in #1534.
 */
object NestImpureMappedInfix extends StatelessTransformer {

  // Are there any impure infixes that exist inside the specified ASTs
  def hasInfix(asts: Ast*): Boolean =
    asts.exists(ast => CollectAst(ast) {
      case i @ Infix(_, _, false, _) => i
    }.nonEmpty)

  // Continue exploring into the Map to see if there are additional impure infix clauses inside.
  private def applyInside(m: Map) =
    Map(apply(m.query), m.alias, m.body)

  override def apply(ast: Ast): Ast =
    ast match {
      // If there is already a nested clause inside the map, there is no reason to insert another one
      case Nested(Map(inner, a, b)) =>
        Nested(Map(apply(inner), a, b))

      case m @ Map(_, x, cc @ CaseClass(values)) if hasInfix(cc) => //Nested(m)
        val newIdent = Ident(x.name, valueQuat(cc.quat))
        Map(Nested(applyInside(m)), newIdent,
          CaseClass(values.map {
            case (name, _) => (name, Property(newIdent, name)) // mappings of nested-query case class properties should not be renamed
          }))

      case m @ Map(_, x, tup @ Tuple(values)) if hasInfix(tup) =>
        val newIdent = Ident(x.name, valueQuat(tup.quat))
        Map(Nested(applyInside(m)), newIdent,
          Tuple(values.zipWithIndex.map {
            case (_, i) => Property(newIdent, s"_${i + 1}") // mappings of nested-query tuple properties should not be renamed
          }))

      case m @ Map(q, x, i @ Infix(_, _, false, _)) =>
        val newMap = Map(apply(q), x, Tuple(List(i)))
        val newIdent = Ident(x.name, valueQuat(newMap.quat))
        Map(Nested(newMap), newIdent, Property(newIdent, "_1"))

      case m @ Map(q, x, i @ Property(prop, _)) if hasInfix(prop) =>
        val newMap = Map(apply(q), x, Tuple(List(i)))
        val newIdent = Ident(x.name, valueQuat(newMap.quat))
        Map(Nested(newMap), newIdent, Property(newIdent, "_1"))

      case m @ Map(q, x, i @ BinaryOperation(a, _, b)) if hasInfix(a, b) =>
        val newMap = Map(apply(q), x, Tuple(List(i)))
        val newIdent = Ident(x.name, valueQuat(newMap.quat))
        Map(Nested(newMap), newIdent, Property(newIdent, "_1"))

      case m @ Map(q, x, i @ UnaryOperation(_, a)) if hasInfix(a) =>
        val newMap = Map(apply(q), x, Tuple(List(i)))
        val newIdent = Ident(x.name, valueQuat(newMap.quat))
        Map(Nested(newMap), newIdent, Property(newIdent, "_1"))

      case other => super.apply(other)
    }
}
