package io.getquill.norm

import io.getquill.ast._

object FlattenOptionOperation extends StatelessTransformer {

  override def apply(ast: Ast) =
    ast match {
      case OptionMap(ast, alias, body) =>
        apply(BetaReduction(body, alias -> ast))
      case OptionForall(ast, alias, body) =>
        val isEmpty = BinaryOperation(ast, EqualityOperator.`==`, NullValue)
        val exists = BetaReduction(body, alias -> ast)
        BinaryOperation(isEmpty, BooleanOperator.`||`, exists)
      case OptionExists(ast, alias, body) =>
        BetaReduction(body, alias -> ast)
      case OptionContains(ast, body) =>
        BinaryOperation(ast, EqualityOperator.`==`, body)
      case other =>
        super.apply(ast)
    }
}
