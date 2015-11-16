package io.getquill.quotation

import io.getquill.util.Messages._
import scala.reflect.macros.whitebox.Context

import io.getquill.ast._

trait Unliftables {
  val c: Context
  import c.universe.{ Ident => _, Constant => _, Function => _, _ }

  implicit val astUnliftable: Unliftable[Ast] = Unliftable[Ast] {
    case queryUnliftable(ast) => ast
    case actionUnliftable(ast) => ast
    case valueUnliftable(ast) => ast
    case identUnliftable(ast) => ast
    case q"$pack.Property.apply(${ a: Ast }, ${ b: String })" => Property(a, b)
    case q"$pack.Function.apply(${ a: List[Ident] }, ${ b: Ast })" => Function(a, b)
    case q"$pack.FunctionApply.apply(${ a: Ast }, ${ b: List[Ast] })" => FunctionApply(a, b)
    case q"$pack.BinaryOperation.apply(${ a: Ast }, ${ b: BinaryOperator }, ${ c: Ast })" => BinaryOperation(a, b, c)
    case q"$pack.UnaryOperation.apply(${ a: UnaryOperator }, ${ b: Ast })" => UnaryOperation(a, b)
    case q"$pack.Aggregation.apply(${ a: AggregationOperator }, ${ b: Ast })" => Aggregation(a, b)
    case q"$pack.Infix.apply(${ a: List[String] }, ${ b: List[Ast] })" => Infix(a, b)
    case q"$pack.OptionOperation.apply(${ a: OptionOperationType }, ${ b: Ast }, ${ c: Ident }, ${ d: Ast })" => OptionOperation(a, b, c, d)
  }

  implicit val optionOperationTypeUnliftable: Unliftable[OptionOperationType] = Unliftable[OptionOperationType] {
    case q"$pack.OptionMap"    => OptionMap
    case q"$pack.OptionForall" => OptionForall
  }

  implicit def listUnliftable[T](implicit u: Unliftable[T]): Unliftable[List[T]] = Unliftable[List[T]] {
    case q"$pack.Nil"                         => Nil
    case q"$pack.List.apply[..$t](..$values)" => values.map(v => u.unapply(v).getOrElse(fail(s"Can't unlift $v")))
  }

  implicit val binaryOperatorUnliftable: Unliftable[BinaryOperator] = Unliftable[BinaryOperator] {
    case q"$pack.EqualityOperator.`==`" => EqualityOperator.`==`
    case q"$pack.EqualityOperator.`!=`" => EqualityOperator.`!=`
    case q"$pack.BooleanOperator.`&&`"  => BooleanOperator.`&&`
    case q"$pack.BooleanOperator.`||`"  => BooleanOperator.`||`
    case q"$pack.StringOperator.`+`"    => StringOperator.`+`
    case q"$pack.NumericOperator.`-`"   => NumericOperator.`-`
    case q"$pack.NumericOperator.`+`"   => NumericOperator.`+`
    case q"$pack.NumericOperator.`*`"   => NumericOperator.`*`
    case q"$pack.NumericOperator.`>`"   => NumericOperator.`>`
    case q"$pack.NumericOperator.`>=`"  => NumericOperator.`>=`
    case q"$pack.NumericOperator.`<`"   => NumericOperator.`<`
    case q"$pack.NumericOperator.`<=`"  => NumericOperator.`<=`
    case q"$pack.NumericOperator.`/`"   => NumericOperator.`/`
    case q"$pack.NumericOperator.`%`"   => NumericOperator.`%`
  }

  implicit val unaryOperatorUnliftable: Unliftable[UnaryOperator] = Unliftable[UnaryOperator] {
    case q"$pack.NumericOperator.`-`"          => NumericOperator.`-`
    case q"$pack.BooleanOperator.`!`"          => BooleanOperator.`!`
    case q"$pack.StringOperator.`toUpperCase`" => StringOperator.`toUpperCase`
    case q"$pack.StringOperator.`toLowerCase`" => StringOperator.`toLowerCase`
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
    case q"$pack.Entity.apply(${ a: String }, ${ b: Option[String] }, ${ c: List[PropertyAlias] })" => Entity(a, b, c)
    case q"$pack.Filter.apply(${ a: Ast }, ${ b: Ident }, ${ c: Ast })" => Filter(a, b, c)
    case q"$pack.Map.apply(${ a: Ast }, ${ b: Ident }, ${ c: Ast })" => Map(a, b, c)
    case q"$pack.FlatMap.apply(${ a: Ast }, ${ b: Ident }, ${ c: Ast })" => FlatMap(a, b, c)
    case q"$pack.SortBy.apply(${ a: Ast }, ${ b: Ident }, ${ c: Ast })" => SortBy(a, b, c)
    case q"$pack.GroupBy.apply(${ a: Ast }, ${ b: Ident }, ${ c: Ast })" => GroupBy(a, b, c)
    case q"$pack.Reverse.apply(${ a: Ast })" => Reverse(a)
    case q"$pack.Take.apply(${ a: Ast }, ${ b: Ast })" => Take(a, b)
    case q"$pack.Drop.apply(${ a: Ast }, ${ b: Ast })" => Drop(a, b)
    case q"$pack.Union.apply(${ a: Ast }, ${ b: Ast })" => Union(a, b)
    case q"$pack.UnionAll.apply(${ a: Ast }, ${ b: Ast })" => UnionAll(a, b)
    case q"$pack.OuterJoin.apply(${ t: OuterJoinType }, ${ a: Ast }, ${ b: Ast }, ${ iA: Ident }, ${ iB: Ident }, ${ on: Ast })" =>
      OuterJoin(t, a, b, iA, iB, on)
  }

  implicit val propertyAliasUnliftable: Unliftable[PropertyAlias] = Unliftable[PropertyAlias] {
    case q"$pack.PropertyAlias.apply(${ a: String }, ${ b: String })" => PropertyAlias(a, b)
  }

  implicit def optionUnliftable[T](implicit u: Unliftable[T]): Unliftable[Option[T]] = Unliftable[Option[T]] {
    case q"scala.None"               => None
    case q"scala.Some.apply[$t]($v)" => Some(u.unapply(v).get)
  }

  implicit val outerJoinTypeUnliftable: Unliftable[OuterJoinType] = Unliftable[OuterJoinType] {
    case q"$pack.LeftJoin"  => LeftJoin
    case q"$pack.RightJoin" => RightJoin
    case q"$pack.FullJoin"  => FullJoin
  }

  implicit val actionUnliftable: Unliftable[Action] = Unliftable[Action] {
    case q"$pack.Update.apply(${ a: Ast }, ${ b: List[Assignment] })" => Update(a, b)
    case q"$pack.Insert.apply(${ a: Ast }, ${ b: List[Assignment] })" => Insert(a, b)
    case q"$pack.Delete.apply(${ a: Ast })"                           => Delete(a)
  }

  implicit val assignmentUnliftable: Unliftable[Assignment] = Unliftable[Assignment] {
    case q"$pack.Assignment.apply(${ a: String }, ${ b: Ast })" => Assignment(a, b)
  }

  implicit val valueUnliftable: Unliftable[Value] = Unliftable[Value] {
    case q"$pack.NullValue" => NullValue
    case q"$pack.Constant.apply(${ Literal(c.universe.Constant(a)) })" => Constant(a)
    case q"$pack.Tuple.apply(${ a: List[Ast] })" => Tuple(a)
  }
  implicit val identUnliftable: Unliftable[Ident] = Unliftable[Ident] {
    case q"$pack.Ident.apply(${ a: String })" => Ident(a)
  }
}
