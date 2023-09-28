package io.getquill.quotation

import scala.reflect.macros.whitebox.Context
import io.getquill.ast._
import io.getquill.quat.Quat

trait Unliftables extends QuatUnliftable {
  val mctx: Context
  import mctx.universe.{Ident => _, Constant => _, Function => _, If => _, _}

  implicit val stringOptionUnliftable: Unliftable[Option[String]] = Unliftable[Option[String]] {
    case q"scala.None"                           => None
    case q"scala.Some[String](${value: String})" => Some(value)
  }

  implicit val astUnliftable: Unliftable[Ast] = Unliftable[Ast] {
    case liftUnliftable(ast)                                                     => ast
    case tagUnliftable(ast)                                                      => ast
    case queryUnliftable(ast)                                                    => ast
    case actionUnliftable(ast)                                                   => ast
    case valueUnliftable(ast)                                                    => ast
    case identUnliftable(ast)                                                    => ast
    case orderingUnliftable(ast)                                                 => ast
    case optionOperationUnliftable(ast)                                          => ast
    case traversableOperationUnliftable(ast)                                     => ast
    case propertyUnliftable(ast)                                                 => ast
    case q"$_.Function.apply(${a: List[Ident]}, ${b: Ast})"                      => Function(a, b)
    case q"$_.FunctionApply.apply(${a: Ast}, ${b: List[Ast]})"                   => FunctionApply(a, b)
    case q"$_.BinaryOperation.apply(${a: Ast}, ${b: BinaryOperator}, ${c: Ast})" => BinaryOperation(a, b, c)
    case q"$_.UnaryOperation.apply(${a: UnaryOperator}, ${b: Ast})"              => UnaryOperation(a, b)
    case q"$_.Aggregation.apply(${a: AggregationOperator}, ${b: Ast})"           => Aggregation(a, b)
    case q"$_.Infix.apply(${a: List[String]}, ${b: List[Ast]}, ${pure: Boolean}, ${transparent: Boolean}, ${quat: Quat})" =>
      Infix(a, b, pure, transparent, quat)
    case q"$_.If.apply(${a: Ast}, ${b: Ast}, ${c: Ast})" => If(a, b, c)
    case q"$_.OnConflict.Excluded.apply(${a: Ident})"    => OnConflict.Excluded(a)
    case q"$_.OnConflict.Existing.apply(${a: Ident})"    => OnConflict.Existing(a)
    case q"$tree.ast"                                    => Dynamic(tree)
  }

  implicit val optionOperationUnliftable: Unliftable[OptionOperation] = Unliftable[OptionOperation] {
    case q"$_.OptionTableFlatMap.apply(${a: Ast}, ${b: Ident}, ${c: Ast})" => OptionTableFlatMap(a, b, c)
    case q"$_.OptionTableMap.apply(${a: Ast}, ${b: Ident}, ${c: Ast})"     => OptionTableMap(a, b, c)
    case q"$_.OptionTableExists.apply(${a: Ast}, ${b: Ident}, ${c: Ast})"  => OptionTableExists(a, b, c)
    case q"$_.OptionTableForall.apply(${a: Ast}, ${b: Ident}, ${c: Ast})"  => OptionTableForall(a, b, c)
    case q"$_.OptionFlatten.apply(${a: Ast})"                              => OptionFlatten(a)
    case q"$_.OptionGetOrElse.apply(${a: Ast}, ${b: Ast})"                 => OptionGetOrElse(a, b)
    case q"$_.OptionOrElse.apply(${a: Ast}, ${b: Ast})"                    => OptionOrElse(a, b)
    case q"$_.OptionFlatMap.apply(${a: Ast}, ${b: Ident}, ${c: Ast})"      => OptionFlatMap(a, b, c)
    case q"$_.OptionMap.apply(${a: Ast}, ${b: Ident}, ${c: Ast})"          => OptionMap(a, b, c)
    case q"$_.OptionForall.apply(${a: Ast}, ${b: Ident}, ${c: Ast})"       => OptionForall(a, b, c)
    case q"$_.OptionExists.apply(${a: Ast}, ${b: Ident}, ${c: Ast})"       => OptionExists(a, b, c)
    case q"$_.OptionContains.apply(${a: Ast}, ${b: Ast})"                  => OptionContains(a, b)
    case q"$_.OptionIsEmpty.apply(${a: Ast})"                              => OptionIsEmpty(a)
    case q"$_.OptionNonEmpty.apply(${a: Ast})"                             => OptionNonEmpty(a)
    case q"$_.OptionIsDefined.apply(${a: Ast})"                            => OptionIsDefined(a)
    case q"$_.OptionSome.apply(${a: Ast})"                                 => OptionSome(a)
    case q"$_.OptionApply.apply(${a: Ast})"                                => OptionApply(a)
    case q"$_.OptionOrNull.apply(${a: Ast})"                               => OptionOrNull(a)
    case q"$_.OptionGetOrNull.apply(${a: Ast})"                            => OptionGetOrNull(a)
    case q"$_.OptionNone.apply(${q: Quat})"                                => OptionNone(q)
    case q"$_.FilterIfDefined.apply(${a: Ast}, ${b: Ident}, ${c: Ast})"    => FilterIfDefined(a, b, c)
  }

  implicit val traversableOperationUnliftable: Unliftable[IterableOperation] = Unliftable[IterableOperation] {
    case q"$_.MapContains.apply(${a: Ast}, ${b: Ast})"  => MapContains(a, b)
    case q"$_.SetContains.apply(${a: Ast}, ${b: Ast})"  => SetContains(a, b)
    case q"$_.ListContains.apply(${a: Ast}, ${b: Ast})" => ListContains(a, b)
  }

  implicit val binaryOperatorUnliftable: Unliftable[BinaryOperator] = Unliftable[BinaryOperator] {
    case q"$_.EqualityOperator.`_==`"      => EqualityOperator.`_==`
    case q"$_.EqualityOperator.`_!=`"      => EqualityOperator.`_!=`
    case q"$_.BooleanOperator.`&&`"        => BooleanOperator.`&&`
    case q"$_.BooleanOperator.`||`"        => BooleanOperator.`||`
    case q"$_.StringOperator.`+`"          => StringOperator.`+`
    case q"$_.StringOperator.`startsWith`" => StringOperator.`startsWith`
    case q"$_.StringOperator.`split`"      => StringOperator.`split`
    case q"$_.NumericOperator.`-`"         => NumericOperator.`-`
    case q"$_.NumericOperator.`+`"         => NumericOperator.`+`
    case q"$_.NumericOperator.`*`"         => NumericOperator.`*`
    case q"$_.NumericOperator.`>`"         => NumericOperator.`>`
    case q"$_.NumericOperator.`>=`"        => NumericOperator.`>=`
    case q"$_.NumericOperator.`<`"         => NumericOperator.`<`
    case q"$_.NumericOperator.`<=`"        => NumericOperator.`<=`
    case q"$_.NumericOperator.`/`"         => NumericOperator.`/`
    case q"$_.NumericOperator.`%`"         => NumericOperator.`%`
    case q"$_.SetOperator.`contains`"      => SetOperator.`contains`
  }

  implicit val unaryOperatorUnliftable: Unliftable[UnaryOperator] = Unliftable[UnaryOperator] {
    case q"$_.NumericOperator.`-`"          => NumericOperator.`-`
    case q"$_.BooleanOperator.`!`"          => BooleanOperator.`!`
    case q"$_.StringOperator.`toUpperCase`" => StringOperator.`toUpperCase`
    case q"$_.StringOperator.`toLowerCase`" => StringOperator.`toLowerCase`
    case q"$_.StringOperator.`toLong`"      => StringOperator.`toLong`
    case q"$_.StringOperator.`toInt`"       => StringOperator.`toInt`
    case q"$_.SetOperator.`nonEmpty`"       => SetOperator.`nonEmpty`
    case q"$_.SetOperator.`isEmpty`"        => SetOperator.`isEmpty`
  }

  implicit val aggregationOperatorUnliftable: Unliftable[AggregationOperator] = Unliftable[AggregationOperator] {
    case q"$_.AggregationOperator.`min`"  => AggregationOperator.`min`
    case q"$_.AggregationOperator.`max`"  => AggregationOperator.`max`
    case q"$_.AggregationOperator.`avg`"  => AggregationOperator.`avg`
    case q"$_.AggregationOperator.`sum`"  => AggregationOperator.`sum`
    case q"$_.AggregationOperator.`size`" => AggregationOperator.`size`
  }

  implicit val queryUnliftable: Unliftable[Query] = Unliftable[Query] {
    case q"$_.Entity.apply(${a: String}, ${b: List[PropertyAlias]}, ${quat: Quat.Product})" => Entity(a, b, quat)
    case q"$_.Entity.Opinionated.apply(${a: String}, ${b: List[PropertyAlias]}, ${quat: Quat.Product}, ${renameable: Renameable})" =>
      Entity.Opinionated(a, b, quat, renameable)
    case q"$_.Filter.apply(${a: Ast}, ${b: Ident}, ${c: Ast})"            => Filter(a, b, c)
    case q"$_.Map.apply(${a: Ast}, ${b: Ident}, ${c: Ast})"               => Map(a, b, c)
    case q"$_.FlatMap.apply(${a: Ast}, ${b: Ident}, ${c: Ast})"           => FlatMap(a, b, c)
    case q"$_.ConcatMap.apply(${a: Ast}, ${b: Ident}, ${c: Ast})"         => ConcatMap(a, b, c)
    case q"$_.SortBy.apply(${a: Ast}, ${b: Ident}, ${c: Ast}, ${d: Ast})" => SortBy(a, b, c, d)
    case q"$_.GroupBy.apply(${a: Ast}, ${b: Ident}, ${c: Ast})"           => GroupBy(a, b, c)
    case q"$_.GroupByMap.apply(${a: Ast}, ${b: Ident}, ${c: Ast}, ${d: Ident}, ${e: Ast})" =>
      GroupByMap(a, b, c, d, e)
    case q"$_.Take.apply(${a: Ast}, ${b: Ast})"     => Take(a, b)
    case q"$_.Drop.apply(${a: Ast}, ${b: Ast})"     => Drop(a, b)
    case q"$_.Union.apply(${a: Ast}, ${b: Ast})"    => Union(a, b)
    case q"$_.UnionAll.apply(${a: Ast}, ${b: Ast})" => UnionAll(a, b)
    case q"$_.Join.apply(${t: JoinType}, ${a: Ast}, ${b: Ast}, ${iA: Ident}, ${iB: Ident}, ${on: Ast})" =>
      Join(t, a, b, iA, iB, on)
    case q"$_.FlatJoin.apply(${t: JoinType}, ${a: Ast}, ${iA: Ident}, ${on: Ast})" =>
      FlatJoin(t, a, iA, on)
    case q"$_.Distinct.apply(${a: Ast})"                           => Distinct(a)
    case q"$_.DistinctOn.apply(${a: Ast}, ${b: Ident}, ${c: Ast})" => DistinctOn(a, b, c)
    case q"$_.Nested.apply(${a: Ast})"                             => Nested(a)
  }

  implicit val orderingUnliftable: Unliftable[Ordering] = Unliftable[Ordering] {
    case q"$_.TupleOrdering.apply(${elems: List[Ordering]})" => TupleOrdering(elems)
    case q"$_.Asc"                                           => Asc
    case q"$_.Desc"                                          => Desc
    case q"$_.AscNullsFirst"                                 => AscNullsFirst
    case q"$_.DescNullsFirst"                                => DescNullsFirst
    case q"$_.AscNullsLast"                                  => AscNullsLast
    case q"$_.DescNullsLast"                                 => DescNullsLast
  }

  implicit val propertyAliasUnliftable: Unliftable[PropertyAlias] = Unliftable[PropertyAlias] {
    case q"$_.PropertyAlias.apply(${a: List[String]}, ${b: String})" => PropertyAlias(a, b)
  }

  implicit val renameableUnliftable: Unliftable[Renameable] = Unliftable[Renameable] {
    case q"$_.Renameable.ByStrategy" => Renameable.ByStrategy
    case q"$_.Renameable.Fixed"      => Renameable.Fixed
  }

  implicit val visibilityUnliftable: Unliftable[Visibility] = Unliftable[Visibility] {
    case q"$_.Visibility.Visible" => Visibility.Visible
    case q"$_.Visibility.Hidden"  => Visibility.Hidden
  }

  implicit val propertyUnliftable: Unliftable[Property] = Unliftable[Property] {
    case q"$_.Property.apply(${a: Ast}, ${b: String})" => Property(a, b)
    case q"$_.Property.Opinionated.apply(${a: Ast}, ${b: String}, ${renameable: Renameable}, ${visibility: Visibility})" =>
      Property.Opinionated(a, b, renameable, visibility)
  }

  implicit def optionUnliftable[T](implicit u: Unliftable[T]): Unliftable[Option[T]] = Unliftable[Option[T]] {
    case q"scala.None"               => None
    case q"scala.Some.apply[$_]($v)" => Some(u.unapply(v).get)
  }

  implicit val joinTypeUnliftable: Unliftable[JoinType] = Unliftable[JoinType] {
    case q"$_.InnerJoin" => InnerJoin
    case q"$_.LeftJoin"  => LeftJoin
    case q"$_.RightJoin" => RightJoin
    case q"$_.FullJoin"  => FullJoin
  }

  implicit val actionUnliftable: Unliftable[Action] = Unliftable[Action] {
    case q"$_.Update.apply(${a: Ast}, ${b: List[Assignment]})"                               => Update(a, b)
    case q"$_.Insert.apply(${a: Ast}, ${b: List[Assignment]})"                               => Insert(a, b)
    case q"$_.Delete.apply(${a: Ast})"                                                       => Delete(a)
    case q"$_.Returning.apply(${a: Ast}, ${b: Ident}, ${c: Ast})"                            => Returning(a, b, c)
    case q"$_.ReturningGenerated.apply(${a: Ast}, ${b: Ident}, ${c: Ast})"                   => ReturningGenerated(a, b, c)
    case q"$_.Foreach.apply(${a: Ast}, ${b: Ident}, ${c: Ast})"                              => Foreach(a, b, c)
    case q"$_.OnConflict.apply(${a: Ast}, ${b: OnConflict.Target}, ${c: OnConflict.Action})" => OnConflict(a, b, c)
  }

  implicit val conflictTargetUnliftable: Unliftable[OnConflict.Target] = Unliftable[OnConflict.Target] {
    case q"$_.OnConflict.NoTarget"                               => OnConflict.NoTarget
    case q"$_.OnConflict.Properties.apply(${a: List[Property]})" => OnConflict.Properties(a)
  }

  implicit val conflictActionUnliftable: Unliftable[OnConflict.Action] = Unliftable[OnConflict.Action] {
    case q"$_.OnConflict.Ignore"                                   => OnConflict.Ignore
    case q"$_.OnConflict.Update.apply(${a: List[AssignmentDual]})" => OnConflict.Update(a)
  }

  implicit val assignmentUnliftable: Unliftable[Assignment] = Unliftable[Assignment] {
    case q"$_.Assignment.apply(${a: Ident}, ${b: Ast}, ${c: Ast})" => Assignment(a, b, c)
  }

  implicit val assignmentDualUnliftable: Unliftable[AssignmentDual] = Unliftable[AssignmentDual] {
    case q"$_.AssignmentDual.apply(${a1: Ident}, ${a2: Ident}, ${b: Ast}, ${c: Ast})" => AssignmentDual(a1, a2, b, c)
  }

  implicit val valueUnliftable: Unliftable[Value] = Unliftable[Value] {
    case q"$_.NullValue"                                                            => NullValue
    case q"$_.Constant.apply(${Literal(mctx.universe.Constant(a))}, ${quat: Quat})" => Constant(a, quat)
    case q"$_.Tuple.apply(${a: List[Ast]})"                                         => Tuple(a)
    case q"$_.CaseClass.apply(${n: String}, ${values: List[(String, Ast)]})"        => CaseClass(n, values)
  }

  implicit val identUnliftable: Unliftable[Ident] = Unliftable[Ident] {
    case q"$_.Ident.apply(${a: String}, ${quat: Quat})" => Ident(a, quat)
  }
  implicit val externalIdentUnliftable: Unliftable[ExternalIdent] = Unliftable[ExternalIdent] {
    case q"$_.ExternalIdent.apply(${a: String}, ${quat: Quat})" => ExternalIdent(a, quat)
  }

  implicit val liftUnliftable: Unliftable[Lift] = Unliftable[Lift] {
    case q"$_.ScalarValueLift.apply(${a: String}, ${a1: External.Source}, $b, $c, ${quat: Quat})" =>
      ScalarValueLift(a, a1, b, c, quat)
    case q"$_.CaseClassValueLift.apply(${a: String}, ${a1: String}, $b, ${quat: Quat})" =>
      CaseClassValueLift(a, a1, b, quat)
    case q"$_.ScalarQueryLift.apply(${a: String}, $b, $c, ${quat: Quat})" => ScalarQueryLift(a, b, c, quat)
    case q"$_.CaseClassQueryLift.apply(${a: String}, $b, ${quat: Quat})"  => CaseClassQueryLift(a, b, quat)
  }
  implicit val sourceUnliftable: Unliftable[External.Source] = Unliftable[External.Source] {
    case q"$_.External.Source.Parser"                                  => External.Source.Parser
    case q"$_.External.Source.UnparsedProperty.apply(${prop: String})" => External.Source.UnparsedProperty(prop)
  }
  implicit val tagUnliftable: Unliftable[Tag] = Unliftable[Tag] {
    case q"$_.ScalarTag.apply(${uid: String}, ${source: External.Source})" => ScalarTag(uid, source)
    case q"$_.QuotationTag.apply(${uid: String})"                          => QuotationTag(uid)
  }
}
