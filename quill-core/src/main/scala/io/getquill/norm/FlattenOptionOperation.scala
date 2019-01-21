package io.getquill.norm

import io.getquill.ast._
import io.getquill.ast.Implicits._

object FlattenOptionOperation extends StatelessTransformer {

  private def emptyOrNot(b: Boolean, ast: Ast) =
    if (b) OptionIsEmpty(ast) else OptionNonEmpty(ast)

  override def apply(ast: Ast): Ast =
    ast match {

      case UncheckedOptionFlatMap(ast, alias, body) =>
        apply(body.reduce(alias -> ast))

      case UncheckedOptionMap(ast, alias, body) =>
        apply(body.reduce(alias -> ast))

      case UncheckedOptionExists(ast, alias, body) =>
        apply(body.reduce(alias -> ast))

      case UncheckedOptionForall(ast, alias, body) =>
        val reduced = body.reduce(alias -> ast)
        apply((Empty(ast) +||+ reduced): Ast)

      case OptionFlatten(ast) =>
        apply(ast)

      case OptionGetOrElse(OptionMap(ast, alias, body), Constant(b: Boolean)) =>
        apply((body.reduce(alias -> ast) +||+ emptyOrNot(b, ast)): Ast)

      case OptionGetOrElse(ast, body) =>
        apply(If(Exist(ast), ast, body))

      case OptionFlatMap(ast, alias, body) =>
        val reduced = body.reduce(alias -> ast)
        apply(IfExistElseNull(ast, reduced))

      case OptionMap(ast, alias, body) =>
        val reduced = body.reduce(alias -> ast)
        apply(IfExistElseNull(ast, reduced))

      case OptionForall(ast, alias, body) =>
        val reduction = body.reduce(alias -> ast)
        apply((Empty(ast) +||+ (Exist(ast) +&&+ reduction)): Ast)

      case OptionExists(ast, alias, body) =>
        val reduction = body.reduce(alias -> ast)
        apply((Exist(ast) +&&+ reduction): Ast)

      case OptionContains(ast, body) =>
        apply((ast +==+ body): Ast)

      case other =>
        super.apply(other)
    }
}
