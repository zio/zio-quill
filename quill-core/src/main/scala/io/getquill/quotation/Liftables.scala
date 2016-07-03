package io.getquill.quotation

import scala.reflect.macros.whitebox.Context

import io.getquill.ast.Action
import io.getquill.ast.Aggregation
import io.getquill.ast.AggregationOperator
import io.getquill.ast.Asc
import io.getquill.ast.AscNullsFirst
import io.getquill.ast.AscNullsLast
import io.getquill.ast.AssignedAction
import io.getquill.ast.Assignment
import io.getquill.ast.Ast
import io.getquill.ast.BinaryOperation
import io.getquill.ast.BinaryOperator
import io.getquill.ast.Block
import io.getquill.ast.BooleanOperator
import io.getquill.ast.Collection
import io.getquill.ast.CompileTimeBinding
import io.getquill.ast.ConfiguredEntity
import io.getquill.ast.Constant
import io.getquill.ast.Delete
import io.getquill.ast.Desc
import io.getquill.ast.DescNullsFirst
import io.getquill.ast.DescNullsLast
import io.getquill.ast.Distinct
import io.getquill.ast.Drop
import io.getquill.ast.Dynamic
import io.getquill.ast.Entity
import io.getquill.ast.EqualityOperator
import io.getquill.ast.Filter
import io.getquill.ast.FlatMap
import io.getquill.ast.FullJoin
import io.getquill.ast.Function
import io.getquill.ast.FunctionApply
import io.getquill.ast.GroupBy
import io.getquill.ast.Ident
import io.getquill.ast.If
import io.getquill.ast.Infix
import io.getquill.ast.InnerJoin
import io.getquill.ast.Insert
import io.getquill.ast.Join
import io.getquill.ast.JoinType
import io.getquill.ast.LeftJoin
import io.getquill.ast.Map
import io.getquill.ast.NullValue
import io.getquill.ast.NumericOperator
import io.getquill.ast.OptionExists
import io.getquill.ast.OptionForall
import io.getquill.ast.OptionMap
import io.getquill.ast.OptionOperation
import io.getquill.ast.OptionOperationType
import io.getquill.ast.Ordering
import io.getquill.ast.Property
import io.getquill.ast.PropertyAlias
import io.getquill.ast.Query
import io.getquill.ast.QuotedReference
import io.getquill.ast.RightJoin
import io.getquill.ast.RuntimeBinding
import io.getquill.ast.SetOperator
import io.getquill.ast.SimpleEntity
import io.getquill.ast.SortBy
import io.getquill.ast.StringOperator
import io.getquill.ast.Take
import io.getquill.ast.Tuple
import io.getquill.ast.TupleOrdering
import io.getquill.ast.UnaryOperation
import io.getquill.ast.UnaryOperator
import io.getquill.ast.Union
import io.getquill.ast.UnionAll
import io.getquill.ast.Update
import io.getquill.ast.Val
import io.getquill.ast.Value
import io.getquill.dsl.CoreDsl

trait Liftables {
  val c: Context
  import c.universe.{ Ident => _, Constant => _, Function => _, If => _, Block => _, _ }

  private val pack = q"io.getquill.ast"

  implicit val astLiftable: Liftable[Ast] = Liftable[Ast] {
    case ast: Query => queryLiftable(ast)
    case ast: Action => actionLiftable(ast)
    case ast: Value => valueLiftable(ast)
    case ast: Ident => identLiftable(ast)
    case ast: Ordering => orderingLiftable(ast)
    case Val(name, body) => q"$pack.Val($name, $body)"
    case Block(statements) => q"$pack.Block($statements)"
    case Property(a, b) => q"$pack.Property($a, $b)"
    case Function(a, b) => q"$pack.Function($a, $b)"
    case FunctionApply(a, b) => q"$pack.FunctionApply($a, $b)"
    case BinaryOperation(a, b, c) => q"$pack.BinaryOperation($a, $b, $c)"
    case UnaryOperation(a, b) => q"$pack.UnaryOperation($a, $b)"
    case Infix(a, b) => q"$pack.Infix($a, $b)"
    case OptionOperation(a, b, c, d) => q"$pack.OptionOperation($a, $b, $c, $d)"
    case If(a, b, c) => q"$pack.If($a, $b, $c)"
    case Dynamic(tree: Tree) if (tree.tpe <:< c.weakTypeOf[CoreDsl#Quoted[Any]]) => q"$tree.ast"
    case Dynamic(tree: Tree) => q"$pack.Constant($tree)"
    case QuotedReference(tree: Tree, ast) => q"$ast"
    case CompileTimeBinding(tree: Tree) => q"$pack.RuntimeBinding(${tree.toString})"
    case RuntimeBinding(name) => q"$pack.RuntimeBinding($name)"
  }

  implicit val optionOperationTypeLiftable: Liftable[OptionOperationType] = Liftable[OptionOperationType] {
    case OptionMap    => q"$pack.OptionMap"
    case OptionForall => q"$pack.OptionForall"
    case OptionExists => q"$pack.OptionExists"
  }

  implicit val binaryOperatorLiftable: Liftable[BinaryOperator] = Liftable[BinaryOperator] {
    case EqualityOperator.`==`  => q"$pack.EqualityOperator.`==`"
    case EqualityOperator.`!=`  => q"$pack.EqualityOperator.`!=`"
    case BooleanOperator.`&&`   => q"$pack.BooleanOperator.`&&`"
    case BooleanOperator.`||`   => q"$pack.BooleanOperator.`||`"
    case StringOperator.`+`     => q"$pack.StringOperator.`+`"
    case NumericOperator.`-`    => q"$pack.NumericOperator.`-`"
    case NumericOperator.`+`    => q"$pack.NumericOperator.`+`"
    case NumericOperator.`*`    => q"$pack.NumericOperator.`*`"
    case NumericOperator.`>`    => q"$pack.NumericOperator.`>`"
    case NumericOperator.`>=`   => q"$pack.NumericOperator.`>=`"
    case NumericOperator.`<`    => q"$pack.NumericOperator.`<`"
    case NumericOperator.`<=`   => q"$pack.NumericOperator.`<=`"
    case NumericOperator.`/`    => q"$pack.NumericOperator.`/`"
    case NumericOperator.`%`    => q"$pack.NumericOperator.`%`"
    case SetOperator.`contains` => q"$pack.SetOperator.`contains`"
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
    case e: Entity              => q"$e"
    case Filter(a, b, c)        => q"$pack.Filter($a, $b, $c)"
    case Map(a, b, c)           => q"$pack.Map($a, $b, $c)"
    case FlatMap(a, b, c)       => q"$pack.FlatMap($a, $b, $c)"
    case SortBy(a, b, c, d)     => q"$pack.SortBy($a, $b, $c, $d)"
    case GroupBy(a, b, c)       => q"$pack.GroupBy($a, $b, $c)"
    case Aggregation(a, b)      => q"$pack.Aggregation($a, $b)"
    case Take(a, b)             => q"$pack.Take($a, $b)"
    case Drop(a, b)             => q"$pack.Drop($a, $b)"
    case Union(a, b)            => q"$pack.Union($a, $b)"
    case UnionAll(a, b)         => q"$pack.UnionAll($a, $b)"
    case Join(a, b, c, d, e, f) => q"$pack.Join($a, $b, $c, $d, $e, $f)"
    case Distinct(a)            => q"$pack.Distinct($a)"
  }

  implicit val entityLiftable: Liftable[Entity] = Liftable[Entity] {
    case SimpleEntity(a)              => q"$pack.SimpleEntity($a)"
    case ConfiguredEntity(a, b, c, d) => q"$pack.ConfiguredEntity($a, $b, $c, $d)"
  }

  implicit val propertyAliasLiftable: Liftable[PropertyAlias] = Liftable[PropertyAlias] {
    case PropertyAlias(a, b) => q"$pack.PropertyAlias($a, $b)"
  }

  implicit val orderingLiftable: Liftable[Ordering] = Liftable[Ordering] {
    case TupleOrdering(elems) => q"$pack.TupleOrdering($elems)"
    case Asc                  => q"$pack.Asc"
    case Desc                 => q"$pack.Desc"
    case AscNullsFirst        => q"$pack.AscNullsFirst"
    case DescNullsFirst       => q"$pack.DescNullsFirst"
    case AscNullsLast         => q"$pack.AscNullsLast"
    case DescNullsLast        => q"$pack.DescNullsLast"
  }

  implicit val joinTypeLiftable: Liftable[JoinType] = Liftable[JoinType] {
    case InnerJoin => q"$pack.InnerJoin"
    case LeftJoin  => q"$pack.LeftJoin"
    case RightJoin => q"$pack.RightJoin"
    case FullJoin  => q"$pack.FullJoin"
  }

  implicit val actionLiftable: Liftable[Action] = Liftable[Action] {
    case AssignedAction(a, b) => q"$pack.AssignedAction($a, $b)"
    case Update(a)            => q"$pack.Update($a)"
    case Insert(a)            => q"$pack.Insert($a)"
    case Delete(a)            => q"$pack.Delete($a)"
  }

  implicit val assignmentLiftable: Liftable[Assignment] = Liftable[Assignment] {
    case Assignment(a, b, c) => q"$pack.Assignment($a, $b, $c)"
  }

  implicit val valueLiftable: Liftable[Value] = Liftable[Value] {
    case NullValue     => q"$pack.NullValue"
    case Constant(a)   => q"$pack.Constant(${Literal(c.universe.Constant(a))})"
    case Tuple(a)      => q"$pack.Tuple($a)"
    case Collection(a) => q"$pack.Collection($a)"
  }
  implicit val identLiftable: Liftable[Ident] = Liftable[Ident] {
    case Ident(a) => q"$pack.Ident($a)"
  }
}
