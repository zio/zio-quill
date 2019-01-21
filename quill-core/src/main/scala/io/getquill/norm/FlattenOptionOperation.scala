package io.getquill.norm

import io.getquill.ast._
import io.getquill.ast.Implicits._

object FlattenOptionOperation extends StatelessTransformer {

  private def emptyOrNot(b: Boolean, ast: Ast) =
    if (b) OptionIsEmpty(ast) else OptionNonEmpty(ast)

  override def apply(ast: Ast): Ast =
    ast match {

      // TODO Check if there is an optional in here, if there is, warn the user about changing behavior

      case OptionTableFlatMap(ast, alias, body) =>
        apply(BetaReduction(body, alias -> ast))

      case OptionTableMap(ast, alias, body) =>
        apply(BetaReduction(body, alias -> ast))

      case OptionTableExists(ast, alias, body) =>
        apply(BetaReduction(body, alias -> ast))

      case OptionTableForall(ast, alias, body) =>
        val reduced = BetaReduction(body, alias -> ast)
        apply((IsNullCheck(ast) +||+ reduced): Ast)

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
        val reduced = BetaReduction(body, alias -> ast)
        apply(IfExistElseNull(ast, reduced))

      case OptionMap(ast, alias, body) =>
        val reduced = BetaReduction(body, alias -> ast)
        apply(IfExistElseNull(ast, reduced))

      case OptionForall(ast, alias, body) =>
        val reduction = BetaReduction(body, alias -> ast)
        apply((IsNullCheck(ast) +||+ (IsNotNullCheck(ast) +&&+ reduction)): Ast)

      case OptionExists(ast, alias, body) =>
        val reduction = BetaReduction(body, alias -> ast)
        apply((IsNotNullCheck(ast) +&&+ reduction): Ast)

      case OptionContains(ast, body) =>
        apply((ast +==+ body): Ast)

      case other =>
        super.apply(other)
    }
}
