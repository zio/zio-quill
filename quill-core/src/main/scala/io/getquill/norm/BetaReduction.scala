package io.getquill.norm

import io.getquill.ast.BinaryOperation
import io.getquill.ast.Ast
import io.getquill.ast.Filter
import io.getquill.ast.FlatMap
import io.getquill.ast.Ident
import io.getquill.ast.Map
import io.getquill.ast.Property
import io.getquill.ast.Query
import io.getquill.ast.Entity
import io.getquill.ast.Tuple
import io.getquill.ast.UnaryOperation
import io.getquill.ast.Value
import io.getquill.ast.StatefulTransformer
import io.getquill.ast.Function
import io.getquill.ast.Operation
import io.getquill.ast.FunctionApply

case class BetaReduction(state: collection.Map[Ident, Ast])
    extends StatefulTransformer[collection.Map[Ident, Ast]] {

  override def apply(ast: Ast) =
    ast match {
      case Property(Tuple(values), name) =>
        apply(values(name.drop(1).toInt - 1))
      case FunctionApply(Function(params, body), values) =>
        BetaReduction(state ++ params.zip(values)).apply(body) match {
          case (body, t) => apply(body)
        }
      case ident: Ident =>
        (state.getOrElse(ident, ident), this)
      case Function(params, body) =>
        val (bodyt, t) = BetaReduction(state -- params)(body)
        (Function(params, bodyt), this)
      case other =>
        super.apply(other)
    }

  override def apply(query: Query) =
    query match {
      case t: Entity =>
        (t, this)
      case Filter(a, b, c) =>
        val (ar, art) = apply(a)
        val (cr, crt) = BetaReduction(state - b)(c)
        (Filter(ar, b, cr), this)
      case Map(a, b, c) =>
        val (ar, art) = apply(a)
        val (cr, crt) = BetaReduction(state - b)(c)
        (Map(ar, b, cr), this)
      case FlatMap(a, b, c) =>
        val (ar, art) = apply(a)
        val (cr, crt) = BetaReduction(state - b)(c)
        (FlatMap(ar, b, cr), this)
    }
}

object BetaReduction {

  def apply(ast: Ast, t: (Ident, Ast)*): Ast =
    BetaReduction(t.toMap)(ast) match {
      case (ast, _) => ast
    }

  def apply(query: Query, t: (Ident, Ast)*): Query =
    BetaReduction(t.toMap)(query) match {
      case (ast, _) => ast
    }
}