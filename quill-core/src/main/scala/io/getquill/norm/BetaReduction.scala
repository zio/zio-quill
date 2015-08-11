package io.getquill.norm

import io.getquill.ast.BinaryOperation
import io.getquill.ast.Expr
import io.getquill.ast.Filter
import io.getquill.ast.FlatMap
import io.getquill.ast.Ident
import io.getquill.ast.Map
import io.getquill.ast.Property
import io.getquill.ast.Query
import io.getquill.ast.Ref
import io.getquill.ast.Table
import io.getquill.ast.Tuple
import io.getquill.ast.UnaryOperation
import io.getquill.ast.Value

object BetaReduction {

  def apply(expr: Expr, t: (Ident, Expr)*): Expr =
    apply(expr)(t.toMap)

  def apply(query: Query, t: (Ident, Expr)*): Query =
    apply(query)(t.toMap)

  def apply(query: Query)(implicit refs: collection.Map[Ident, Expr]): Query =
    query match {
      case t: Table =>
        t
      case Filter(source, alias, body) =>
        Filter(apply(source), alias, apply(body)(refs - alias))
      case Map(source, alias, body) =>
        Map(apply(source), alias, apply(body)(refs - alias))
      case FlatMap(source, alias, body) =>
        FlatMap(apply(source), alias, apply(body)(refs - alias))
    }

  def apply(expr: Expr)(implicit refs: collection.Map[Ident, Expr]): Expr =
    expr match {
      case expr: Ref                 => apply(expr)
      case UnaryOperation(op, expr)  => UnaryOperation(op, apply(expr))
      case BinaryOperation(a, op, b) => BinaryOperation(apply(a), op, apply(b))
    }

  def apply(ref: Ref)(implicit refs: collection.Map[Ident, Expr]): Expr =
    ref match {
      case Property(Tuple(values), name) => values(name.drop(1).toInt - 1)
      case Tuple(values)                 => Tuple(values.map(apply))
      case Property(ref, name)           => Property(apply(ref), name)
      case ident: Ident                  => refs.getOrElse(ident, ident)
      case value: Value                  => value
    }
}