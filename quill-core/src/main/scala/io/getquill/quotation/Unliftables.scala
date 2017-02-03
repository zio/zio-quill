package io.getquill.quotation

import scala.reflect.macros.whitebox.Context

import io.getquill.ast._
import io.getquill.util.Messages.RichContext

trait Unliftables {
  val c: Context
  import c.universe.{ Ident => _, Constant => _, Function => _, If => _, _ }

  implicit val astUnliftable: Unliftable[Ast] = Unliftable[Ast] {
    case liftUnliftable(ast) => ast
    case queryUnliftable(ast) => ast
    case actionUnliftable(ast) => ast
    case valueUnliftable(ast) => ast
    case identUnliftable(ast) => ast
    case orderingUnliftable(ast) => ast
    case optionOperationUnliftable(ast) => ast
    case q"$pack.Property.apply(${ a: Ast }, ${ b: String })" => Property(a, b)
    case q"$pack.Function.apply(${ a: List[Ident] }, ${ b: Ast })" => Function(a, b)
    case q"$pack.FunctionApply.apply(${ a: Ast }, ${ b: List[Ast] })" => FunctionApply(a, b)
    case q"$pack.BinaryOperation.apply(${ a: Ast }, ${ b: BinaryOperator }, ${ c: Ast })" => BinaryOperation(a, b, c)
    case q"$pack.UnaryOperation.apply(${ a: UnaryOperator }, ${ b: Ast })" => UnaryOperation(a, b)
    case q"$pack.Aggregation.apply(${ a: AggregationOperator }, ${ b: Ast })" => Aggregation(a, b)
    case q"$pack.Infix.apply(${ a: List[String] }, ${ b: List[Ast] })" => Infix(a, b)
    case q"$pack.If.apply(${ a: Ast }, ${ b: Ast }, ${ c: Ast })" => If(a, b, c)
    case q"$tree.ast" => Dynamic(tree)
  }

  implicit val optionOperationUnliftable: Unliftable[OptionOperation] = Unliftable[OptionOperation] {
    case q"$pack.OptionMap.apply(${ a: Ast }, ${ b: Ident }, ${ c: Ast })"    => OptionMap(a, b, c)
    case q"$pack.OptionForall.apply(${ a: Ast }, ${ b: Ident }, ${ c: Ast })" => OptionForall(a, b, c)
    case q"$pack.OptionExists.apply(${ a: Ast }, ${ b: Ident }, ${ c: Ast })" => OptionExists(a, b, c)
    case q"$pack.OptionContains.apply(${ a: Ast }, ${ b: Ast })"              => OptionContains(a, b)
  }

  implicit def listUnliftable[T](implicit u: Unliftable[T]): Unliftable[List[T]] = Unliftable[List[T]] {
    case q"$pack.Nil"                         => Nil
    case q"$pack.List.apply[..$t](..$values)" => values.map(v => u.unapply(v).getOrElse(c.fail(s"Can't unlift $v")))
  }

  implicit val binaryOperatorUnliftable: Unliftable[BinaryOperator] = Unliftable[BinaryOperator] {
    case q"$pack.EqualityOperator.`==`"  => EqualityOperator.`==`
    case q"$pack.EqualityOperator.`!=`"  => EqualityOperator.`!=`
    case q"$pack.BooleanOperator.`&&`"   => BooleanOperator.`&&`
    case q"$pack.BooleanOperator.`||`"   => BooleanOperator.`||`
    case q"$pack.StringOperator.`+`"     => StringOperator.`+`
    case q"$pack.NumericOperator.`-`"    => NumericOperator.`-`
    case q"$pack.NumericOperator.`+`"    => NumericOperator.`+`
    case q"$pack.NumericOperator.`*`"    => NumericOperator.`*`
    case q"$pack.NumericOperator.`>`"    => NumericOperator.`>`
    case q"$pack.NumericOperator.`>=`"   => NumericOperator.`>=`
    case q"$pack.NumericOperator.`<`"    => NumericOperator.`<`
    case q"$pack.NumericOperator.`<=`"   => NumericOperator.`<=`
    case q"$pack.NumericOperator.`/`"    => NumericOperator.`/`
    case q"$pack.NumericOperator.`%`"    => NumericOperator.`%`
    case q"$pack.SetOperator.`contains`" => SetOperator.`contains`
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
    case q"$pack.Entity.apply(${ a: String }, ${ b: List[PropertyAlias] })"          => Entity(a, b)
    case q"$pack.Filter.apply(${ a: Ast }, ${ b: Ident }, ${ c: Ast })"              => Filter(a, b, c)
    case q"$pack.Map.apply(${ a: Ast }, ${ b: Ident }, ${ c: Ast })"                 => Map(a, b, c)
    case q"$pack.FlatMap.apply(${ a: Ast }, ${ b: Ident }, ${ c: Ast })"             => FlatMap(a, b, c)
    case q"$pack.SortBy.apply(${ a: Ast }, ${ b: Ident }, ${ c: Ast }, ${ d: Ast })" => SortBy(a, b, c, d)
    case q"$pack.GroupBy.apply(${ a: Ast }, ${ b: Ident }, ${ c: Ast })"             => GroupBy(a, b, c)
    case q"$pack.Take.apply(${ a: Ast }, ${ b: Ast })"                               => Take(a, b)
    case q"$pack.Drop.apply(${ a: Ast }, ${ b: Ast })"                               => Drop(a, b)
    case q"$pack.Union.apply(${ a: Ast }, ${ b: Ast })"                              => Union(a, b)
    case q"$pack.UnionAll.apply(${ a: Ast }, ${ b: Ast })"                           => UnionAll(a, b)
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
    case q"$pack.Update.apply(${ a: Ast }, ${ b: List[Assignment] })"      => Update(a, b)
    case q"$pack.Insert.apply(${ a: Ast }, ${ b: List[Assignment] })"      => Insert(a, b)
    case q"$pack.Delete.apply(${ a: Ast })"                                => Delete(a)
    case q"$pack.Returning.apply(${ a: Ast }, ${ b: Ident }, ${ c: Ast })" => Returning(a, b, c)
    case q"$pack.Foreach.apply(${ a: Ast }, ${ b: Ident }, ${ c: Ast })"   => Foreach(a, b, c)
  }

  implicit val assignmentUnliftable: Unliftable[Assignment] = Unliftable[Assignment] {
    case q"$pack.Assignment.apply(${ a: Ident }, ${ b: Ast }, ${ c: Ast })" => Assignment(a, b, c)
  }

  implicit val valueUnliftable: Unliftable[Value] = Unliftable[Value] {
    case q"$pack.NullValue" => NullValue
    case q"$pack.Constant.apply(${ Literal(c.universe.Constant(a)) })" => Constant(a)
    case q"$pack.Tuple.apply(${ a: List[Ast] })" => Tuple(a)
  }
  implicit val identUnliftable: Unliftable[Ident] = Unliftable[Ident] {
    case q"$pack.Ident.apply(${ a: String })" => Ident(a)
  }

  implicit val liftUnliftable: Unliftable[Lift] = Unliftable[Lift] {
    case q"$pack.ScalarValueLift.apply(${ a: String }, $b, $c)" => ScalarValueLift(a, b, c)
    case q"$pack.CaseClassValueLift.apply(${ a: String }, $b)"  => CaseClassValueLift(a, b)
    case q"$pack.ScalarQueryLift.apply(${ a: String }, $b, $c)" => ScalarQueryLift(a, b, c)
    case q"$pack.CaseClassQueryLift.apply(${ a: String }, $b)"  => CaseClassQueryLift(a, b)
  }
}
