package io.getquill.norm

import io.getquill.ast.Ref
import io.getquill.ast._

object BetaReduction {

  def apply(query: Query)(implicit refs: collection.Map[Ident, Expr]): Query =
    query match {
      case t: Table =>
        t
      case Filter(source, alias, body) =>
        Filter(apply(source), alias, apply(body))
      case Map(source, alias, body) =>
        Map(apply(source), alias, apply(body))
      case FlatMap(source, alias, body) =>
        FlatMap(apply(source), alias, apply(body))
    }

  def apply(expr: Expr)(implicit refs: collection.Map[Ident, Expr]): Expr =
    expr match {
      case expr: Predicate => apply(expr)
      case expr: Ref       => apply(expr)
      case Subtract(a, b)  => Subtract(apply(a), apply(b))
      case Add(a, b)       => Add(apply(a), apply(b))
    }

  def apply(predicate: Predicate)(implicit refs: collection.Map[Ident, Expr]): Predicate =
    predicate match {
      case Equals(a, b) =>
        Equals(apply(a), apply(b))
      case And(a, b) =>
        And(apply(a), apply(b))
      case GreaterThan(a, b) =>
        GreaterThan(apply(a), apply(b))
      case GreaterThanOrEqual(a, b) =>
        GreaterThanOrEqual(apply(a), apply(b))
      case LessThan(a, b) =>
        LessThan(apply(a), apply(b))
      case LessThanOrEqual(a, b) =>
        LessThanOrEqual(apply(a), apply(b))
    }

  def apply(ref: Ref)(implicit refs: collection.Map[Ident, Expr]): Expr =
    ref match {
      case Tuple(values) =>
        Tuple(values.map(apply))
      case Property(ref, name) =>
        apply(ref) match {
          case Tuple(values) =>
            values(name.drop(1).toInt - 1)
          case expr =>
            Property(expr, name)
        }
      case ident: Ident if (refs.contains(ident)) =>
        refs(ident)
      case ident: Ident =>
        ident
      case value: Value =>
        value
    }
}