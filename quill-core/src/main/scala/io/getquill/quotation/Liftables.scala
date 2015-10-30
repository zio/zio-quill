package io.getquill.quotation

import scala.reflect.macros.whitebox.Context

import io.getquill.ast._

trait Liftables {
  val c: Context
  import c.universe.{ Ident => _, Constant => _, Function => _, _ }

  private val pack = q"io.getquill.ast"

  implicit val astLiftable: Liftable[Ast] = Liftable[Ast] {
    case ast: Query                  => queryLiftable(ast)
    case ast: Action                 => actionLiftable(ast)
    case ast: Value                  => valueLiftable(ast)
    case ast: Ident                  => identLiftable(ast)
    case Property(a, b)              => q"$pack.Property($a, $b)"
    case Function(a, b)              => q"$pack.Function($a, $b)"
    case FunctionApply(a, b)         => q"$pack.FunctionApply($a, $b)"
    case BinaryOperation(a, b, c)    => q"$pack.BinaryOperation($a, $b, $c)"
    case UnaryOperation(a, b)        => q"$pack.UnaryOperation($a, $b)"
    case Infix(a, b)                 => q"$pack.Infix($a, $b)"
    case OptionOperation(a, b, c, d) => q"$pack.OptionOperation($a, $b, $c, $d)"
  }

  implicit val optionOperationTypeLiftable: Liftable[OptionOperationType] = Liftable[OptionOperationType] {
    case OptionMap    => q"$pack.OptionMap"
    case OptionForall => q"$pack.OptionForall"
  }

  implicit val binaryOperatorLiftable: Liftable[BinaryOperator] = Liftable[BinaryOperator] {
    case EqualityOperator.`==` => q"$pack.EqualityOperator.`==`"
    case EqualityOperator.`!=` => q"$pack.EqualityOperator.`!=`"
    case BooleanOperator.`&&`  => q"$pack.BooleanOperator.`&&`"
    case BooleanOperator.`||`  => q"$pack.BooleanOperator.`||`"
    case StringOperator.`+`    => q"$pack.StringOperator.`+`"
    case NumericOperator.`-`   => q"$pack.NumericOperator.`-`"
    case NumericOperator.`+`   => q"$pack.NumericOperator.`+`"
    case NumericOperator.`*`   => q"$pack.NumericOperator.`*`"
    case NumericOperator.`>`   => q"$pack.NumericOperator.`>`"
    case NumericOperator.`>=`  => q"$pack.NumericOperator.`>=`"
    case NumericOperator.`<`   => q"$pack.NumericOperator.`<`"
    case NumericOperator.`<=`  => q"$pack.NumericOperator.`<=`"
    case NumericOperator.`/`   => q"$pack.NumericOperator.`/`"
    case NumericOperator.`%`   => q"$pack.NumericOperator.`%`"
  }

  implicit val unaryOperatorLiftable: Liftable[UnaryOperator] = Liftable[UnaryOperator] {
    case NumericOperator.`-`          => q"$pack.NumericOperator.`-`"
    case BooleanOperator.`!`          => q"$pack.BooleanOperator.`!`"
    case StringOperator.`toUpperCase` => q"$pack.StringOperator.`toUpperCase`"
    case StringOperator.`toLowerCase` => q"$pack.StringOperator.`toLowerCase`"
    case SetOperator.`nonEmpty`       => q"$pack.SetOperator.`nonEmpty`"
    case SetOperator.`isEmpty`        => q"$pack.SetOperator.`isEmpty`"
  }

  implicit val aggregationOperatorLiftable: Liftable[AggregationOperator] = Liftable[AggregationOperator] {
    case AggregationOperator.`min`  => q"$pack.AggregationOperator.`min`"
    case AggregationOperator.`max`  => q"$pack.AggregationOperator.`max`"
    case AggregationOperator.`avg`  => q"$pack.AggregationOperator.`avg`"
    case AggregationOperator.`sum`  => q"$pack.AggregationOperator.`sum`"
    case AggregationOperator.`size` => q"$pack.AggregationOperator.`size`"
  }

  implicit val queryLiftable: Liftable[Query] = Liftable[Query] {
    case Entity(a, b, c)             => q"$pack.Entity($a, $b, $c)"
    case Filter(a, b, c)             => q"$pack.Filter($a, $b, $c)"
    case Map(a, b, c)                => q"$pack.Map($a, $b, $c)"
    case FlatMap(a, b, c)            => q"$pack.FlatMap($a, $b, $c)"
    case SortBy(a, b, c)             => q"$pack.SortBy($a, $b, $c)"
    case GroupBy(a, b, c)            => q"$pack.GroupBy($a, $b, $c)"
    case Aggregation(a, b)           => q"$pack.Aggregation($a, $b)"
    case Reverse(a)                  => q"$pack.Reverse($a)"
    case Take(a, b)                  => q"$pack.Take($a, $b)"
    case Drop(a, b)                  => q"$pack.Drop($a, $b)"
    case Union(a, b)                 => q"$pack.Union($a, $b)"
    case UnionAll(a, b)              => q"$pack.UnionAll($a, $b)"
    case OuterJoin(a, b, c, d, e, f) => q"$pack.OuterJoin($a, $b, $c, $d, $e, $f)"
  }

  implicit val propertyAliasLiftable: Liftable[PropertyAlias] = Liftable[PropertyAlias] {
    case PropertyAlias(a, b) => q"$pack.PropertyAlias($a, $b)"
  }

  implicit val outerJoinTypeLiftable: Liftable[OuterJoinType] = Liftable[OuterJoinType] {
    case LeftJoin  => q"$pack.LeftJoin"
    case RightJoin => q"$pack.RightJoin"
    case FullJoin  => q"$pack.FullJoin"
  }

  implicit val actionLiftable: Liftable[Action] = Liftable[Action] {
    case Update(a, b) => q"$pack.Update($a, $b)"
    case Insert(a, b) => q"$pack.Insert($a, $b)"
    case Delete(a)    => q"$pack.Delete($a)"
  }

  implicit val assignmentLiftable: Liftable[Assignment] = Liftable[Assignment] {
    case Assignment(a, b) => q"$pack.Assignment($a, $b)"
  }

  implicit val valueLiftable: Liftable[Value] = Liftable[Value] {
    case NullValue   => q"$pack.NullValue"
    case Constant(a) => q"$pack.Constant(${Literal(c.universe.Constant(a))})"
    case Tuple(a)    => q"$pack.Tuple($a)"
  }
  implicit val identLiftable: Liftable[Ident] = Liftable[Ident] {
    case Ident(a) => q"$pack.Ident($a)"
  }
}
