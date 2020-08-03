package io.getquill.norm

import io.getquill.ast.Implicits.AstOpsExt
import io.getquill.ast.{ Ast, BinaryOperation, Constant, Filter, If, Query, StatelessTransformer }
import io.getquill.quat.Quat.{ BooleanExpression, BooleanValue }

object VendorizeBooleans extends StatelessTransformer {

  //  def valuefyExpression(ast: Ast): Ast = ast.quat match {
  //    case Quat.BooleanExpression => ast
  //    case Quat.BooleanValue      => ast
  //    case _                      => ast
  //  }

  def valuefyExpression(ast: Ast): Ast = ast match {
    case Constant(v, quat) =>
      quat match {
        case BooleanExpression => ast
        case BooleanValue      => If(ast, Constant(true, BooleanValue), Constant(false, BooleanValue))
        case _                 => ast
      }
    case _ => ast
  }

  def expressifyValue(ast: Ast): Ast = ast.quat match {
    case BooleanExpression => ast
    case BooleanValue      => Constant(true) +==+ ast
    case _                 => ast
  }

  override def apply(q: Query): Query =
    q match {
      case Filter(q, alias, BinaryOperation(a, op, b)) =>
        Filter(q, alias, BinaryOperation(expressifyValue(a), op, expressifyValue(b)))
      case Filter(q, alias, body) =>
        Filter(q, alias, expressifyValue(apply(body)))
      case _ => super.apply(q)
    }
}
