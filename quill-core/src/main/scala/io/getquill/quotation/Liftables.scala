package io.getquill.quotation

import scala.reflect.macros.whitebox.Context

import io.getquill.ast._
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
    case ast: ExternalIdent => externalIdentLiftable(ast)
    case ast: Ordering => orderingLiftable(ast)
    case ast: Lift => liftLiftable(ast)
    case ast: Assignment => assignmentLiftable(ast)
    case ast: OptionOperation => optionOperationLiftable(ast)
    case ast: IterableOperation => traversableOperationLiftable(ast)
    case ast: Property => propertyLiftable(ast)
    case Val(name, body) => q"$pack.Val($name, $body)"
    case Block(statements) => q"$pack.Block($statements)"
    case Function(a, b) => q"$pack.Function($a, $b)"
    case FunctionApply(a, b) => q"$pack.FunctionApply($a, $b)"
    case BinaryOperation(a, b, c) => q"$pack.BinaryOperation($a, $b, $c)"
    case UnaryOperation(a, b) => q"$pack.UnaryOperation($a, $b)"
    case Infix(a, b, pure) => q"$pack.Infix($a, $b, $pure)"
    case If(a, b, c) => q"$pack.If($a, $b, $c)"
    case Dynamic(tree: Tree) if (tree.tpe <:< c.weakTypeOf[CoreDsl#Quoted[Any]]) => q"$tree.ast"
    case Dynamic(tree: Tree) => q"$pack.Constant($tree)"
    case QuotedReference(tree: Tree, ast) => q"$ast"
    case OnConflict.Excluded(a) => q"$pack.OnConflict.Excluded($a)"
    case OnConflict.Existing(a) => q"$pack.OnConflict.Existing($a)"
  }

  implicit val optionOperationLiftable: Liftable[OptionOperation] = Liftable[OptionOperation] {
    case OptionTableFlatMap(a, b, c) => q"$pack.OptionTableFlatMap($a,$b,$c)"
    case OptionTableMap(a, b, c)     => q"$pack.OptionTableMap($a,$b,$c)"
    case OptionTableExists(a, b, c)  => q"$pack.OptionTableExists($a,$b,$c)"
    case OptionTableForall(a, b, c)  => q"$pack.OptionTableForall($a,$b,$c)"
    case OptionFlatten(a)            => q"$pack.OptionFlatten($a)"
    case OptionGetOrElse(a, b)       => q"$pack.OptionGetOrElse($a,$b)"
    case OptionFlatMap(a, b, c)      => q"$pack.OptionFlatMap($a,$b,$c)"
    case OptionMap(a, b, c)          => q"$pack.OptionMap($a,$b,$c)"
    case OptionForall(a, b, c)       => q"$pack.OptionForall($a,$b,$c)"
    case OptionExists(a, b, c)       => q"$pack.OptionExists($a,$b,$c)"
    case OptionContains(a, b)        => q"$pack.OptionContains($a,$b)"
    case OptionIsEmpty(a)            => q"$pack.OptionIsEmpty($a)"
    case OptionNonEmpty(a)           => q"$pack.OptionNonEmpty($a)"
    case OptionIsDefined(a)          => q"$pack.OptionIsDefined($a)"
    case OptionSome(a)               => q"$pack.OptionSome($a)"
    case OptionApply(a)              => q"$pack.OptionApply($a)"
    case OptionOrNull(a)             => q"$pack.OptionOrNull($a)"
    case OptionGetOrNull(a)          => q"$pack.OptionGetOrNull($a)"
    case OptionNone                  => q"$pack.OptionNone"
  }

  implicit val traversableOperationLiftable: Liftable[IterableOperation] = Liftable[IterableOperation] {
    case MapContains(a, b)  => q"$pack.MapContains($a,$b)"
    case SetContains(a, b)  => q"$pack.SetContains($a,$b)"
    case ListContains(a, b) => q"$pack.ListContains($a,$b)"
  }

  implicit val binaryOperatorLiftable: Liftable[BinaryOperator] = Liftable[BinaryOperator] {
    case EqualityOperator.`==`       => q"$pack.EqualityOperator.`==`"
    case EqualityOperator.`!=`       => q"$pack.EqualityOperator.`!=`"
    case BooleanOperator.`&&`        => q"$pack.BooleanOperator.`&&`"
    case BooleanOperator.`||`        => q"$pack.BooleanOperator.`||`"
    case StringOperator.`+`          => q"$pack.StringOperator.`+`"
    case StringOperator.`startsWith` => q"$pack.StringOperator.`startsWith`"
    case StringOperator.`split`      => q"$pack.StringOperator.`split`"
    case NumericOperator.`-`         => q"$pack.NumericOperator.`-`"
    case NumericOperator.`+`         => q"$pack.NumericOperator.`+`"
    case NumericOperator.`*`         => q"$pack.NumericOperator.`*`"
    case NumericOperator.`>`         => q"$pack.NumericOperator.`>`"
    case NumericOperator.`>=`        => q"$pack.NumericOperator.`>=`"
    case NumericOperator.`<`         => q"$pack.NumericOperator.`<`"
    case NumericOperator.`<=`        => q"$pack.NumericOperator.`<=`"
    case NumericOperator.`/`         => q"$pack.NumericOperator.`/`"
    case NumericOperator.`%`         => q"$pack.NumericOperator.`%`"
    case SetOperator.`contains`      => q"$pack.SetOperator.`contains`"
  }

  implicit val unaryOperatorLiftable: Liftable[UnaryOperator] = Liftable[UnaryOperator] {
    case NumericOperator.`-`          => q"$pack.NumericOperator.`-`"
    case BooleanOperator.`!`          => q"$pack.BooleanOperator.`!`"
    case StringOperator.`toUpperCase` => q"$pack.StringOperator.`toUpperCase`"
    case StringOperator.`toLowerCase` => q"$pack.StringOperator.`toLowerCase`"
    case StringOperator.`toLong`      => q"$pack.StringOperator.`toLong`"
    case StringOperator.`toInt`       => q"$pack.StringOperator.`toInt`"
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

  implicit val renameableLiftable: Liftable[Renameable] = Liftable[Renameable] {
    case Renameable.Fixed      => q"$pack.Renameable.Fixed"
    case Renameable.ByStrategy => q"$pack.Renameable.ByStrategy"
  }

  implicit val visibilityLiftable: Liftable[Visibility] = Liftable[Visibility] {
    case Visibility.Visible => q"$pack.Visibility.Visible"
    case Visibility.Hidden  => q"$pack.Visibility.Hidden"
  }

  implicit val queryLiftable: Liftable[Query] = Liftable[Query] {
    case Entity.Opinionated(a, b, renameable) => q"$pack.Entity.Opinionated($a, $b, $renameable)"
    case Filter(a, b, c)                      => q"$pack.Filter($a, $b, $c)"
    case Map(a, b, c)                         => q"$pack.Map($a, $b, $c)"
    case FlatMap(a, b, c)                     => q"$pack.FlatMap($a, $b, $c)"
    case ConcatMap(a, b, c)                   => q"$pack.ConcatMap($a, $b, $c)"
    case SortBy(a, b, c, d)                   => q"$pack.SortBy($a, $b, $c, $d)"
    case GroupBy(a, b, c)                     => q"$pack.GroupBy($a, $b, $c)"
    case Aggregation(a, b)                    => q"$pack.Aggregation($a, $b)"
    case Take(a, b)                           => q"$pack.Take($a, $b)"
    case Drop(a, b)                           => q"$pack.Drop($a, $b)"
    case Union(a, b)                          => q"$pack.Union($a, $b)"
    case UnionAll(a, b)                       => q"$pack.UnionAll($a, $b)"
    case Join(a, b, c, d, e, f)               => q"$pack.Join($a, $b, $c, $d, $e, $f)"
    case FlatJoin(a, b, c, d)                 => q"$pack.FlatJoin($a, $b, $c, $d)"
    case Distinct(a)                          => q"$pack.Distinct($a)"
    case Nested(a)                            => q"$pack.Nested($a)"
  }

  implicit val propertyAliasLiftable: Liftable[PropertyAlias] = Liftable[PropertyAlias] {
    case PropertyAlias(a, b) => q"$pack.PropertyAlias($a, $b)"
  }

  implicit val propertyLiftable: Liftable[Property] = Liftable[Property] {
    case Property.Opinionated(a, b, renameable, visibility) => q"$pack.Property.Opinionated($a, $b, $renameable, $visibility)"
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
    case Update(a, b)                => q"$pack.Update($a, $b)"
    case Insert(a, b)                => q"$pack.Insert($a, $b)"
    case Delete(a)                   => q"$pack.Delete($a)"
    case Returning(a, b, c)          => q"$pack.Returning($a, $b, $c)"
    case ReturningGenerated(a, b, c) => q"$pack.ReturningGenerated($a, $b, $c)"
    case Foreach(a, b, c)            => q"$pack.Foreach($a, $b, $c)"
    case OnConflict(a, b, c)         => q"$pack.OnConflict($a, $b, $c)"
  }

  implicit val conflictTargetLiftable: Liftable[OnConflict.Target] = Liftable[OnConflict.Target] {
    case OnConflict.NoTarget      => q"$pack.OnConflict.NoTarget"
    case OnConflict.Properties(a) => q"$pack.OnConflict.Properties.apply($a)"
  }

  implicit val conflictActionLiftable: Liftable[OnConflict.Action] = Liftable[OnConflict.Action] {
    case OnConflict.Ignore    => q"$pack.OnConflict.Ignore"
    case OnConflict.Update(a) => q"$pack.OnConflict.Update.apply($a)"
  }

  implicit val assignmentLiftable: Liftable[Assignment] = Liftable[Assignment] {
    case Assignment(a, b, c) => q"$pack.Assignment($a, $b, $c)"
  }

  implicit val valueLiftable: Liftable[Value] = Liftable[Value] {
    case NullValue    => q"$pack.NullValue"
    case Constant(a)  => q"$pack.Constant(${Literal(c.universe.Constant(a))})"
    case Tuple(a)     => q"$pack.Tuple($a)"
    case CaseClass(a) => q"$pack.CaseClass($a)"
  }
  implicit val identLiftable: Liftable[Ident] = Liftable[Ident] {
    case Ident(a) => q"$pack.Ident($a)"
  }
  implicit val externalIdentLiftable: Liftable[ExternalIdent] = Liftable[ExternalIdent] {
    case ExternalIdent(a) => q"$pack.ExternalIdent($a)"
  }

  implicit val liftLiftable: Liftable[Lift] = Liftable[Lift] {
    case ScalarValueLift(a, b: Tree, c: Tree) => q"$pack.ScalarValueLift($a, $b, $c)"
    case CaseClassValueLift(a, b: Tree)       => q"$pack.CaseClassValueLift($a, $b)"
    case ScalarQueryLift(a, b: Tree, c: Tree) => q"$pack.ScalarQueryLift($a, $b, $c)"
    case CaseClassQueryLift(a, b: Tree)       => q"$pack.CaseClassQueryLift($a, $b)"
  }
}
