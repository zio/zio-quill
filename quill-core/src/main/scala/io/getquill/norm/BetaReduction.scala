package io.getquill.norm

import io.getquill.ast.BinaryOperation
import io.getquill.ast.Ast
import io.getquill.ast.Filter
import io.getquill.ast.FlatMap
import io.getquill.ast.Ident
import io.getquill.ast.Map
import io.getquill.ast.Property
import io.getquill.ast.Query
import io.getquill.ast.Table
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
        (values(name.drop(1).toInt - 1), this)
      case FunctionApply(Function(params, body), values) =>
        BetaReduction(state ++ params.zip(values))(body)
      case ident: Ident =>
        (state.getOrElse(ident, ident), this)
      case other =>
        super.apply(other)
    }

  override def apply(function: Function) =
    function match {
      case Function(params, body) =>
        val (bodyt, t) = BetaReduction(state -- params)(body)
        (Function(params, bodyt), t)
    }

  override def apply(query: Query) =
    query match {
      case t: Table =>
        (t, this)
      case Filter(a, b, c) =>
        val (ar, art) = apply(a)
        val (cr, crt) = BetaReduction(art.state - b)(c)
        (Filter(ar, b, cr), crt)
      case Map(a, b, c) =>
        val (ar, art) = apply(a)
        val (cr, crt) = BetaReduction(art.state - b)(c)
        (Map(ar, b, cr), crt)
      case FlatMap(a, b, c) =>
        val (ar, art) = apply(a)
        val (cr, crt) = BetaReduction(art.state - b)(c)
        (FlatMap(ar, b, cr), crt)
    }
}

object BetaReduction {

  def apply(ast: Ast, t: (Ident, Ast)*): Ast =
    BetaReduction(t.toMap)(ast)._1

  def apply(query: Query, t: (Ident, Ast)*): Query =
    BetaReduction(t.toMap)(query)._1
}