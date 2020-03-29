package io.getquill.norm

import io.getquill.ast._
import io.getquill.ast.Implicits._
import io.getquill.norm.ConcatBehavior.NonAnsiConcat

class FlattenOptionOperation(concatBehavior: ConcatBehavior) extends StatelessTransformer {

  private def emptyOrNot(b: Boolean, ast: Ast) =
    if (b) OptionIsEmpty(ast) else OptionNonEmpty(ast)

  def uncheckedReduction(ast: Ast, alias: Ident, body: Ast) =
    apply(BetaReduction(body, alias -> ast))

  def uncheckedForall(ast: Ast, alias: Ident, body: Ast) = {
    val reduced = BetaReduction(body, alias -> ast)
    apply((IsNullCheck(ast) +||+ reduced): Ast)
  }

  def containsNonFallthroughElement(ast: Ast) =
    CollectAst(ast) {
      case If(_, _, _) => true
      case Infix(_, _, _) => true
      case BinaryOperation(_, StringOperator.`+`, _) if (concatBehavior == NonAnsiConcat) => true
    }.nonEmpty

  override def apply(ast: Ast): Ast =
    ast match {

      case OptionTableFlatMap(ast, alias, body) =>
        uncheckedReduction(ast, alias, body)

      case OptionTableMap(ast, alias, body) =>
        uncheckedReduction(ast, alias, body)

      case OptionTableExists(ast, alias, body) =>
        uncheckedReduction(ast, alias, body)

      case OptionTableForall(ast, alias, body) =>
        uncheckedForall(ast, alias, body)

      case OptionFlatten(ast) =>
        apply(ast)

      case OptionSome(ast) =>
        apply(ast)

      case OptionApply(ast) =>
        apply(ast)

      case OptionOrNull(ast) =>
        apply(ast)

      case OptionGetOrNull(ast) =>
        apply(ast)

      case OptionNone => NullValue

      case OptionGetOrElse(OptionMap(ast, alias, body), Constant(b: Boolean)) =>
        apply((BetaReduction(body, alias -> ast) +||+ emptyOrNot(b, ast)): Ast)

      case OptionGetOrElse(ast, body) =>
        apply(If(IsNotNullCheck(ast), ast, body))

      case OptionFlatMap(ast, alias, body) =>
        if (containsNonFallthroughElement(body)) {
          val reduced = BetaReduction(body, alias -> ast)
          apply(IfExistElseNull(ast, reduced))
        } else {
          uncheckedReduction(ast, alias, body)
        }

      case OptionMap(ast, alias, body) =>
        if (containsNonFallthroughElement(body)) {
          val reduced = BetaReduction(body, alias -> ast)
          apply(IfExistElseNull(ast, reduced))
        } else {
          uncheckedReduction(ast, alias, body)
        }

      case OptionForall(ast, alias, body) =>
        if (containsNonFallthroughElement(body)) {
          val reduction = BetaReduction(body, alias -> ast)
          apply((IsNullCheck(ast) +||+ (IsNotNullCheck(ast) +&&+ reduction)): Ast)
        } else {
          uncheckedForall(ast, alias, body)
        }

      case OptionExists(ast, alias, body) =>
        if (containsNonFallthroughElement(body)) {
          val reduction = BetaReduction(body, alias -> ast)
          apply((IsNotNullCheck(ast) +&&+ reduction): Ast)
        } else {
          uncheckedReduction(ast, alias, body)
        }

      case OptionContains(ast, body) =>
        apply((ast +==+ body): Ast)

      case other =>
        super.apply(other)
    }
}
