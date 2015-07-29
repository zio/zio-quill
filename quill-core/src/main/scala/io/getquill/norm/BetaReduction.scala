package io.getquill.norm

import io.getquill.ast.Add
import io.getquill.ast.And
import io.getquill.ast.Division
import io.getquill.ast.Equals
import io.getquill.ast.Expr
import io.getquill.ast.Filter
import io.getquill.ast.FlatMap
import io.getquill.ast.GreaterThan
import io.getquill.ast.GreaterThanOrEqual
import io.getquill.ast.Ident
import io.getquill.ast.LessThan
import io.getquill.ast.LessThanOrEqual
import io.getquill.ast.Map
import io.getquill.ast.Property
import io.getquill.ast.Query
import io.getquill.ast.Ref
import io.getquill.ast.Remainder
import io.getquill.ast.Subtract
import io.getquill.ast.Table
import io.getquill.ast.Tuple
import io.getquill.ast.Value

object BetaReduction {

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
      case expr: Ref                => apply(expr)
      case Subtract(a, b)           => Subtract(apply(a), apply(b))
      case Division(a, b)           => Division(apply(a), apply(b))
      case Remainder(a, b)          => Remainder(apply(a), apply(b))
      case Add(a, b)                => Add(apply(a), apply(b))
      case Equals(a, b)             => Equals(apply(a), apply(b))
      case And(a, b)                => And(apply(a), apply(b))
      case GreaterThan(a, b)        => GreaterThan(apply(a), apply(b))
      case GreaterThanOrEqual(a, b) => GreaterThanOrEqual(apply(a), apply(b))
      case LessThan(a, b)           => LessThan(apply(a), apply(b))
      case LessThanOrEqual(a, b)    => LessThanOrEqual(apply(a), apply(b))
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