package io.getquill.norm

import io.getquill.ast._

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
      case i @ Infix(_, _, false) => i
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
        Map(Nested(applyInside(m)), x,
          CaseClass(values.map {
            case (name, _) => (name, Property(x, name)) // mappings of nested-query case class properties should not be renamed
          }))

      case m @ Map(_, x, tup @ Tuple(values)) if hasInfix(tup) =>
        Map(Nested(applyInside(m)), x,
          Tuple(values.zipWithIndex.map {
            case (_, i) => Property(x, s"_${i + 1}") // mappings of nested-query tuple properties should not be renamed
          }))

      case m @ Map(_, x, i @ Infix(_, _, false)) =>
        Map(Nested(applyInside(m)), x, Property(x, "_1"))

      case m @ Map(_, x, Property(prop, _)) if hasInfix(prop) =>
        Map(Nested(applyInside(m)), x, Property(x, "_1"))

      case m @ Map(_, x, BinaryOperation(a, _, b)) if hasInfix(a, b) =>
        Map(Nested(applyInside(m)), x, Property(x, "_1"))

      case m @ Map(_, x, UnaryOperation(_, a)) if hasInfix(a) =>
        Map(Nested(applyInside(m)), x, Property(x, "_1"))

      case other => super.apply(other)
    }
}
