package io.getquill.quotation

import scala.reflect.macros.whitebox.Context
import io.getquill.ast._
import io.getquill.quat.Quat

trait Unliftables extends QuatUnliftable {
  val mctx: Context
  import mctx.universe.{ Ident => _, Constant => _, Function => _, If => _, _ }

  implicit val astUnliftable: Unliftable[Ast] = Unliftable[Ast] {
    case liftUnliftable(ast) => ast
    case queryUnliftable(ast) => ast
    case actionUnliftable(ast) => ast
    case valueUnliftable(ast) => ast
    case identUnliftable(ast) => ast
    case orderingUnliftable(ast) => ast
    case optionOperationUnliftable(ast) => ast
    case traversableOperationUnliftable(ast) => ast
    case propertyUnliftable(ast) => ast
    case q"$pack.Function.apply(${ a: List[Ident] }, ${ b: Ast })" => Function(a, b)
    case q"$pack.FunctionApply.apply(${ a: Ast }, ${ b: List[Ast] })" => FunctionApply(a, b)
    case q"$pack.BinaryOperation.apply(${ a: Ast }, ${ b: BinaryOperator }, ${ c: Ast })" => BinaryOperation(a, b, c)
    case q"$pack.UnaryOperation.apply(${ a: UnaryOperator }, ${ b: Ast })" => UnaryOperation(a, b)
    case q"$pack.Aggregation.apply(${ a: AggregationOperator }, ${ b: Ast })" => Aggregation(a, b)
    case q"$pack.Infix.apply(${ a: List[String] }, ${ b: List[Ast] }, ${ pure: Boolean }, ${ quat: Quat })" => Infix(a, b, pure, quat)
    case q"$pack.If.apply(${ a: Ast }, ${ b: Ast }, ${ c: Ast })" => If(a, b, c)
    case q"$pack.OnConflict.Excluded.apply(${ a: Ident })" => OnConflict.Excluded(a)
    case q"$pack.OnConflict.Existing.apply(${ a: Ident })" => OnConflict.Existing(a)
    case q"$tree.ast" => Dynamic(tree)
  }

  implicit val optionOperationUnliftable: Unliftable[OptionOperation] = Unliftable[OptionOperation] {
    case q"$pack.OptionTableFlatMap.apply(${ a: Ast }, ${ b: Ident }, ${ c: Ast })" => OptionTableFlatMap(a, b, c)
    case q"$pack.OptionTableMap.apply(${ a: Ast }, ${ b: Ident }, ${ c: Ast })"     => OptionTableMap(a, b, c)
    case q"$pack.OptionTableExists.apply(${ a: Ast }, ${ b: Ident }, ${ c: Ast })"  => OptionTableExists(a, b, c)
    case q"$pack.OptionTableForall.apply(${ a: Ast }, ${ b: Ident }, ${ c: Ast })"  => OptionTableForall(a, b, c)
    case q"$pack.OptionFlatten.apply(${ a: Ast })"                                  => OptionFlatten(a)
    case q"$pack.OptionGetOrElse.apply(${ a: Ast }, ${ b: Ast })"                   => OptionGetOrElse(a, b)
    case q"$pack.OptionFlatMap.apply(${ a: Ast }, ${ b: Ident }, ${ c: Ast })"      => OptionFlatMap(a, b, c)
    case q"$pack.OptionMap.apply(${ a: Ast }, ${ b: Ident }, ${ c: Ast })"          => OptionMap(a, b, c)
    case q"$pack.OptionForall.apply(${ a: Ast }, ${ b: Ident }, ${ c: Ast })"       => OptionForall(a, b, c)
    case q"$pack.OptionExists.apply(${ a: Ast }, ${ b: Ident }, ${ c: Ast })"       => OptionExists(a, b, c)
    case q"$pack.OptionContains.apply(${ a: Ast }, ${ b: Ast })"                    => OptionContains(a, b)
    case q"$pack.OptionIsEmpty.apply(${ a: Ast })"                                  => OptionIsEmpty(a)
    case q"$pack.OptionNonEmpty.apply(${ a: Ast })"                                 => OptionNonEmpty(a)
    case q"$pack.OptionIsDefined.apply(${ a: Ast })"                                => OptionIsDefined(a)
    case q"$pack.OptionSome.apply(${ a: Ast })"                                     => OptionSome(a)
    case q"$pack.OptionApply.apply(${ a: Ast })"                                    => OptionApply(a)
    case q"$pack.OptionOrNull.apply(${ a: Ast })"                                   => OptionOrNull(a)
    case q"$pack.OptionGetOrNull.apply(${ a: Ast })"                                => OptionGetOrNull(a)
    case q"$pack.OptionNone.apply(${ q: Quat })"                                    => OptionNone(q)
  }

  implicit val traversableOperationUnliftable: Unliftable[IterableOperation] = Unliftable[IterableOperation] {
    case q"$pack.MapContains.apply(${ a: Ast }, ${ b: Ast })"  => MapContains(a, b)
    case q"$pack.SetContains.apply(${ a: Ast }, ${ b: Ast })"  => SetContains(a, b)
    case q"$pack.ListContains.apply(${ a: Ast }, ${ b: Ast })" => ListContains(a, b)
  }

  implicit val binaryOperatorUnliftable: Unliftable[BinaryOperator] = Unliftable[BinaryOperator] {
    case q"$pack.EqualityOperator.`==`"       => EqualityOperator.`==`
    case q"$pack.EqualityOperator.`!=`"       => EqualityOperator.`!=`
    case q"$pack.BooleanOperator.`&&`"        => BooleanOperator.`&&`
    case q"$pack.BooleanOperator.`||`"        => BooleanOperator.`||`
    case q"$pack.StringOperator.`+`"          => StringOperator.`+`
    case q"$pack.StringOperator.`startsWith`" => StringOperator.`startsWith`
    case q"$pack.StringOperator.`split`"      => StringOperator.`split`
    case q"$pack.NumericOperator.`-`"         => NumericOperator.`-`
    case q"$pack.NumericOperator.`+`"         => NumericOperator.`+`
    case q"$pack.NumericOperator.`*`"         => NumericOperator.`*`
    case q"$pack.NumericOperator.`>`"         => NumericOperator.`>`
    case q"$pack.NumericOperator.`>=`"        => NumericOperator.`>=`
    case q"$pack.NumericOperator.`<`"         => NumericOperator.`<`
    case q"$pack.NumericOperator.`<=`"        => NumericOperator.`<=`
    case q"$pack.NumericOperator.`/`"         => NumericOperator.`/`
    case q"$pack.NumericOperator.`%`"         => NumericOperator.`%`
    case q"$pack.SetOperator.`contains`"      => SetOperator.`contains`
  }

  implicit val unaryOperatorUnliftable: Unliftable[UnaryOperator] = Unliftable[UnaryOperator] {
    case q"$pack.NumericOperator.`-`"          => NumericOperator.`-`
    case q"$pack.BooleanOperator.`!`"          => BooleanOperator.`!`
    case q"$pack.StringOperator.`toUpperCase`" => StringOperator.`toUpperCase`
    case q"$pack.StringOperator.`toLowerCase`" => StringOperator.`toLowerCase`
    case q"$pack.StringOperator.`toLong`"      => StringOperator.`toLong`
    case q"$pack.StringOperator.`toInt`"       => StringOperator.`toInt`
    case q"$pack.SetOperator.`nonEmpty`"       => SetOperator.`nonEmpty`
    case q"$pack.SetOperator.`isEmpty`"        => SetOperator.`isEmpty`
  }

  implicit val aggregationOperatorUnliftable: Unliftable[AggregationOperator] = Unliftable[AggregationOperator] {
    case q"$pack.AggregationOperator.`min`"  => AggregationOperator.`min`
    case q"$pack.AggregationOperator.`max`"  => AggregationOperator.`max`
    case q"$pack.AggregationOperator.`avg`"  => AggregationOperator.`avg`
    case q"$pack.AggregationOperator.`sum`"  => AggregationOperator.`sum`
    case q"$pack.AggregationOperator.`size`" => AggregationOperator.`size`
  }

  implicit val queryUnliftable: Unliftable[Query] = Unliftable[Query] {
    case q"$pack.Entity.apply(${ a: String }, ${ b: List[PropertyAlias] }, ${ quat: Quat.Product })" => Entity(a, b, quat)
    case q"$pack.Entity.Opinionated.apply(${ a: String }, ${ b: List[PropertyAlias] }, ${ quat: Quat.Product }, ${ renameable: Renameable })" => Entity.Opinionated(a, b, quat, renameable)
    case q"$pack.Filter.apply(${ a: Ast }, ${ b: Ident }, ${ c: Ast })" => Filter(a, b, c)
    case q"$pack.Map.apply(${ a: Ast }, ${ b: Ident }, ${ c: Ast })" => Map(a, b, c)
    case q"$pack.FlatMap.apply(${ a: Ast }, ${ b: Ident }, ${ c: Ast })" => FlatMap(a, b, c)
    case q"$pack.ConcatMap.apply(${ a: Ast }, ${ b: Ident }, ${ c: Ast })" => ConcatMap(a, b, c)
    case q"$pack.SortBy.apply(${ a: Ast }, ${ b: Ident }, ${ c: Ast }, ${ d: Ast })" => SortBy(a, b, c, d)
    case q"$pack.GroupBy.apply(${ a: Ast }, ${ b: Ident }, ${ c: Ast })" => GroupBy(a, b, c)
    case q"$pack.Take.apply(${ a: Ast }, ${ b: Ast })" => Take(a, b)
    case q"$pack.Drop.apply(${ a: Ast }, ${ b: Ast })" => Drop(a, b)
    case q"$pack.Union.apply(${ a: Ast }, ${ b: Ast })" => Union(a, b)
    case q"$pack.UnionAll.apply(${ a: Ast }, ${ b: Ast })" => UnionAll(a, b)
    case q"$pack.Join.apply(${ t: JoinType }, ${ a: Ast }, ${ b: Ast }, ${ iA: Ident }, ${ iB: Ident }, ${ on: Ast })" =>
      Join(t, a, b, iA, iB, on)
    case q"$pack.FlatJoin.apply(${ t: JoinType }, ${ a: Ast }, ${ iA: Ident }, ${ on: Ast })" =>
      FlatJoin(t, a, iA, on)
    case q"$pack.Distinct.apply(${ a: Ast })" => Distinct(a)
    case q"$pack.Nested.apply(${ a: Ast })"   => Nested(a)
  }

  implicit val orderingUnliftable: Unliftable[Ordering] = Unliftable[Ordering] {
    case q"$pack.TupleOrdering.apply(${ elems: List[Ordering] })" => TupleOrdering(elems)
    case q"$pack.Asc" => Asc
    case q"$pack.Desc" => Desc
    case q"$pack.AscNullsFirst" => AscNullsFirst
    case q"$pack.DescNullsFirst" => DescNullsFirst
    case q"$pack.AscNullsLast" => AscNullsLast
    case q"$pack.DescNullsLast" => DescNullsLast
  }

  implicit val propertyAliasUnliftable: Unliftable[PropertyAlias] = Unliftable[PropertyAlias] {
    case q"$pack.PropertyAlias.apply(${ a: List[String] }, ${ b: String })" => PropertyAlias(a, b)
  }

  implicit val renameableUnliftable: Unliftable[Renameable] = Unliftable[Renameable] {
    case q"$pack.Renameable.ByStrategy" => Renameable.ByStrategy
    case q"$pack.Renameable.Fixed"      => Renameable.Fixed
  }

  implicit val visibilityUnliftable: Unliftable[Visibility] = Unliftable[Visibility] {
    case q"$pack.Visibility.Visible" => Visibility.Visible
    case q"$pack.Visibility.Hidden"  => Visibility.Hidden
  }

  implicit val propertyUnliftable: Unliftable[Property] = Unliftable[Property] {
    case q"$pack.Property.apply(${ a: Ast }, ${ b: String })" => Property(a, b)
    case q"$pack.Property.Opinionated.apply(${ a: Ast }, ${ b: String }, ${ renameable: Renameable }, ${ visibility: Visibility })" => Property.Opinionated(a, b, renameable, visibility)
  }

  implicit def optionUnliftable[T](implicit u: Unliftable[T]): Unliftable[Option[T]] = Unliftable[Option[T]] {
    case q"scala.None"               => None
    case q"scala.Some.apply[$t]($v)" => Some(u.unapply(v).get)
  }

  implicit val joinTypeUnliftable: Unliftable[JoinType] = Unliftable[JoinType] {
    case q"$pack.InnerJoin" => InnerJoin
    case q"$pack.LeftJoin"  => LeftJoin
    case q"$pack.RightJoin" => RightJoin
    case q"$pack.FullJoin"  => FullJoin
  }

  implicit val actionUnliftable: Unliftable[Action] = Unliftable[Action] {
    case q"$pack.Update.apply(${ a: Ast }, ${ b: List[Assignment] })" => Update(a, b)
    case q"$pack.Insert.apply(${ a: Ast }, ${ b: List[Assignment] })" => Insert(a, b)
    case q"$pack.Delete.apply(${ a: Ast })" => Delete(a)
    case q"$pack.Returning.apply(${ a: Ast }, ${ b: Ident }, ${ c: Ast })" => Returning(a, b, c)
    case q"$pack.ReturningGenerated.apply(${ a: Ast }, ${ b: Ident }, ${ c: Ast })" => ReturningGenerated(a, b, c)
    case q"$pack.Foreach.apply(${ a: Ast }, ${ b: Ident }, ${ c: Ast })" => Foreach(a, b, c)
    case q"$pack.OnConflict.apply(${ a: Ast }, ${ b: OnConflict.Target }, ${ c: OnConflict.Action })" => OnConflict(a, b, c)
  }

  implicit val conflictTargetUnliftable: Unliftable[OnConflict.Target] = Unliftable[OnConflict.Target] {
    case q"$pack.OnConflict.NoTarget"                                 => OnConflict.NoTarget
    case q"$pack.OnConflict.Properties.apply(${ a: List[Property] })" => OnConflict.Properties(a)
  }

  implicit val conflictActionUnliftable: Unliftable[OnConflict.Action] = Unliftable[OnConflict.Action] {
    case q"$pack.OnConflict.Ignore"                                 => OnConflict.Ignore
    case q"$pack.OnConflict.Update.apply(${ a: List[Assignment] })" => OnConflict.Update(a)
  }

  implicit val assignmentUnliftable: Unliftable[Assignment] = Unliftable[Assignment] {
    case q"$pack.Assignment.apply(${ a: Ident }, ${ b: Ast }, ${ c: Ast })" => Assignment(a, b, c)
  }

  implicit val valueUnliftable: Unliftable[Value] = Unliftable[Value] {
    case q"$pack.NullValue" => NullValue
    case q"$pack.Constant.apply(${ Literal(mctx.universe.Constant(a)) }, ${ quat: Quat })" => Constant(a, quat)
    case q"$pack.Tuple.apply(${ a: List[Ast] })" => Tuple(a)
    case q"$pack.CaseClass.apply(${ values: List[(String, Ast)] })" => CaseClass(values)
  }

  implicit val identUnliftable: Unliftable[Ident] = Unliftable[Ident] {
    case q"$pack.Ident.apply(${ a: String }, ${ quat: Quat })" => Ident(a, quat)
  }
  implicit val externalIdentUnliftable: Unliftable[ExternalIdent] = Unliftable[ExternalIdent] {
    case q"$pack.ExternalIdent.apply(${ a: String }, ${ quat: Quat })" => ExternalIdent(a, quat)
  }

  implicit val liftUnliftable: Unliftable[Lift] = Unliftable[Lift] {
    case q"$pack.ScalarValueLift.apply(${ a: String }, $b, $c, ${ quat: Quat })" => ScalarValueLift(a, b, c, quat)
    case q"$pack.CaseClassValueLift.apply(${ a: String }, $b, ${ quat: Quat })"  => CaseClassValueLift(a, b, quat)
    case q"$pack.ScalarQueryLift.apply(${ a: String }, $b, $c, ${ quat: Quat })" => ScalarQueryLift(a, b, c, quat)
    case q"$pack.CaseClassQueryLift.apply(${ a: String }, $b, ${ quat: Quat })"  => CaseClassQueryLift(a, b, quat)
  }
  implicit val tagUnliftable: Unliftable[Tag] = Unliftable[Tag] {
    case q"$pack.ScalarTag.apply(${ uid: String })"    => ScalarTag(uid)
    case q"$pack.QuotationTag.apply(${ uid: String })" => QuotationTag(uid)
  }
}
