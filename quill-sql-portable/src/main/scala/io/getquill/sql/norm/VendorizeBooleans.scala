package io.getquill.sql.norm

import io.getquill.ast.Implicits.AstOpsExt
import io.getquill.ast._
import io.getquill.quat.Quat.{ BooleanExpression, BooleanValue }

object VendorizeBooleans extends StatelessTransformer {

  override def apply(ast: Ast): Ast =
    ast match {
      case Filter(q, alias, op: BinaryOperation) =>
        Filter(apply(q), alias, apply(op))
      case Filter(q, alias, body) =>
        Filter(apply(q), alias, expressifyValue(body))
      case If(cond, t, e) =>
        If(expressifyValue(apply(cond)), valuefyExpression(apply(t)), valuefyExpression(apply(e)))
      case Join(typ, a, b, aliasA, aliasB, on: Constant) =>
        Join(typ, a, b, aliasA, aliasB, Constant(on.v, BooleanExpression))
      case _ =>
        super.apply(ast)
    }

  override def apply(operation: Operation): Operation =
    operation match {
      case BinaryOperation(a: Constant, op, b: Constant) =>
        BinaryOperation(expressifyValue(a), op, expressifyValue(b))
      case BinaryOperation(a, op, b) =>
        BinaryOperation(apply(a), op, apply(b))
      case _ =>
        super.apply(operation)
    }

  def expressifyValue(ast: Ast): Ast = ast.quat match {
    case BooleanExpression => ast
    case BooleanValue      => Constant(true) +==+ ast
    case _                 => apply(ast)
  }

  def valuefyExpression(ast: Ast): Ast = ast.quat match {
    case BooleanValue      => ast
    case BooleanExpression => If(ast, Constant(true, BooleanValue), Constant(false, BooleanValue))
    case _                 => apply(ast)
  }
}
