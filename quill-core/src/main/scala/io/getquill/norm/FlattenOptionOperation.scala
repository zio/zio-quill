package io.getquill.norm

import io.getquill.ast._

object FlattenOptionOperation extends StatelessTransformer {

  override def apply(ast: Ast) =
    ast match {
      case OptionMap(ast, alias, body) =>
        apply(BetaReduction(body, alias -> ast))
      case OptionForall(ast, alias, body) =>
        val isEmpty = apply(BinaryOperation(ast, EqualityOperator.`==`, NullValue): Ast)
        val exists = apply(BetaReduction(body, alias -> ast))
        BinaryOperation(isEmpty, BooleanOperator.`||`, exists)
      case OptionExists(ast, alias, body) =>
        apply(BetaReduction(body, alias -> ast))
      case OptionContains(ast, body) =>
        BinaryOperation(ast, EqualityOperator.`==`, body)
      case other =>
        super.apply(other)
    }
}
