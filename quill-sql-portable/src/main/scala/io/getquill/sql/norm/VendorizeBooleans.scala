package io.getquill.sql.norm

import io.getquill.ast.Implicits.AstOpsExt
import io.getquill.ast._
import io.getquill.quat.Quat.{ BooleanExpression, BooleanValue }

object VendorizeBooleans extends StatelessTransformer {

  override def apply(ast: Ast): Ast =
    ast match {
      case Filter(q, alias, body) =>
        Filter(apply(q), alias, expressifyValue(apply(body)))
      case If(cond, t, e) =>
        If(expressifyValue(apply(cond)), valuefyExpression(apply(t)), valuefyExpression(apply(e)))
      case Join(typ, a, b, aliasA, aliasB, on: Constant) =>
        Join(typ, apply(a), apply(b), aliasA, aliasB, expressifyValue(apply(on: Ast)))
      case FlatJoin(typ, a, aliasA, on) =>
        FlatJoin(typ, a, aliasA, expressifyValue(apply(on)))
      case _ =>
        super.apply(ast)
    }

  object OperatorOnExpressions {
    import BooleanOperator._

    def unapply(op: BinaryOperator) =
      op match {
        case `||` | `&&` => Some(op)
        case _           => None
      }
  }

  object OperatorOnValues {
    import NumericOperator._

    def unapply(op: BinaryOperator) =
      op match {
        case `<` | `>` | `<=` | `>=` | EqualityOperator.`==` | EqualityOperator.`!=` => Some(op)
        case _ => None
      }
  }

  object StringTransformerOperation {
    import StringOperator._

    def unapply(op: UnaryOperation) =
      op.operator match {
        case `toUpperCase` | `toLowerCase` | `toLong` | `toInt` => Some(op)
        case _ => None
      }
  }

  override def apply(operation: Operation): Operation = {
    import BooleanOperator._

    operation match {
      // Things that have ||, && between them are typically expressions, things like "true || e.isSomething"
      // need to be converted to "true == true || e.isSomething == true" so they are
      // tokenized as "1 == 1 || e.isSomething == 1"
      case BinaryOperation(a, OperatorOnExpressions(op), b) =>
        BinaryOperation(expressifyValue(apply(a)), op, expressifyValue(apply(b)))

      // Things that have ==,!=,<,>,<=,>= are typically values, things like "true == e.isSomething"
      // need to be converted to "if (true == e.isSomething) true else false" so they are
      // tokenized as "if (1 == e.isSomething) 1 else 0".
      // Operations transforming strings are an exception to the role and should not be valuefied
      case BinaryOperation(a, OperatorOnValues(op), b) => (a, b) match {
        case (StringTransformerOperation(_), StringTransformerOperation(_)) =>
          BinaryOperation(apply(a), op, apply(b))
        case (StringTransformerOperation(_), _) =>
          BinaryOperation(apply(a), op, valuefyExpression(apply(b)))
        case (_, StringTransformerOperation(_)) =>
          BinaryOperation(valuefyExpression(apply(a)), op, apply(b))
        case _ =>
          BinaryOperation(valuefyExpression(apply(a)), op, valuefyExpression(apply(b)))
      }

      // Example: "q.filter(e => !e.isSomething)" which needs to be converted to
      // "q.filter(e => !(e.isSomething == 1))" so it can be tokenized to "... WHERE e.isSomething = 1
      case UnaryOperation(`!`, ast) =>
        UnaryOperation(`!`, expressifyValue(apply(ast)))

      case _ =>
        super.apply(operation)
    }
  }

  def expressifyValue(ast: Ast): Ast = ast.quat match {
    case BooleanValue => Constant(true, BooleanValue) +==+ ast
    case _            => ast
  }

  def valuefyExpression(ast: Ast): Ast = ast.quat match {
    case BooleanExpression => If(ast, Constant(true, BooleanValue), Constant(false, BooleanValue))
    case _                 => ast
  }
}
