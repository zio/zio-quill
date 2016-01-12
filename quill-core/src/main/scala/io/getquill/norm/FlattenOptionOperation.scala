package io.getquill.norm

import io.getquill.ast.StatelessTransformer
import io.getquill.ast.Ast
import io.getquill.ast.OptionOperation

object FlattenOptionOperation extends StatelessTransformer {

  override def apply(ast: Ast) =
    ast match {
      case OptionOperation(t, ast, alias, body) =>
        BetaReduction(body, alias -> ast)
      case other =>
        super.apply(ast)
    }
}
