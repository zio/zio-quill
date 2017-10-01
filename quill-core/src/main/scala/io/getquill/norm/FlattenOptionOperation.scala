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
        apply(BinaryOperation(isEmpty, BooleanOperator.`||`, exists): Ast)
      case OptionExists(ast, alias, body) =>
        apply(BetaReduction(body, alias -> ast))
      case OptionContains(ast, body) =>
        apply(BinaryOperation(ast, EqualityOperator.`==`, body): Ast)
      case other =>
        super.apply(other)
    }
}
