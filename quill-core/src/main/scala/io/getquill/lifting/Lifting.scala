package io.getquill.lifting

import scala.reflect.macros.whitebox.Context
import io.getquill.ast._

trait Lifting {
  val c: Context
  import c.universe.{ Function => _, Expr => _, Ident => _, Constant => _, _ }

  private val pack = q"io.getquill.ast"

  implicit val queryLift: Liftable[Query] = Liftable[Query] {
    case Table(name) =>
      q"$pack.Table($name)"
    case Filter(query, alias, body) =>
      q"$pack.Filter($query, $alias, $body)"
    case Map(query, alias, body) =>
      q"$pack.Map($query, $alias, $body)"
    case FlatMap(query, alias, body) =>
      q"$pack.FlatMap($query, $alias, $body)"
  }

  implicit val exprLift: Liftable[Expr] = Liftable[Expr] {
    case Subtract(a, b) =>
      q"$pack.Subtract($a, $b)"
    case Add(a, b) =>
      q"$pack.Add($a, $b)"
    case predicate: Predicate =>
      q"$predicate"
    case ref: Ref =>
      q"$ref"
  }

  implicit val predicateLift: Liftable[Predicate] = Liftable[Predicate] {
    case Equals(a, b) =>
      q"$pack.Equals($a, $b)"
    case And(a, b) =>
      q"$pack.And($a, $b)"
    case GreaterThan(a, b) =>
      q"$pack.GreaterThan($a, $b)"
    case GreaterThanOrEqual(a, b) =>
      q"$pack.GreaterThanOrEqual($a, $b)"
    case LessThan(a, b) =>
      q"$pack.LessThan($a, $b)"
    case LessThanOrEqual(a, b) =>
      q"$pack.LessThanOrEqual($a, $b)"
  }

  implicit val refLift: Liftable[Ref] = Liftable[Ref] {
    case Property(ref, name) =>
      q"$pack.Property($ref, $name)"
    case Ident(ident) =>
      q"$pack.Ident($ident)"
    case v: Value =>
      q"$v"
  }

  implicit val valueLift: Liftable[Value] = Liftable[Value] {
    case Constant(v) =>
      q"$pack.Constant(${Literal(c.universe.Constant(v))})"
    case NullValue =>
      q"$pack.NullValue"
    case Tuple(values) =>
      q"$pack.Tuple(List(..$values))"
  }

  implicit val identLift: Liftable[Ident] = Liftable[Ident] {
    case Ident(name) =>
      q"$pack.Ident($name)"
  }

  implicit val parametrizedLift: Liftable[Parametrized] = Liftable[Parametrized] {
    case ParametrizedQuery(params, query) =>
      q"$pack.ParametrizedQuery(List(..$params), $query)"
    case ParametrizedExpr(params, expr) =>
      q"$pack.ParametrizedExpr(List(..$params), $expr)"
  }
}