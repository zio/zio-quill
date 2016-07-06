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
import io.getquill.ast.BooleanOperator
import io.getquill.ast.Collection
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
import io.getquill.ast.Value
import io.getquill.util.Messages.RichContext

trait Unliftables {
  val c: Context
  import c.universe.{ Ident => _, Constant => _, Function => _, If => _, _ }

  implicit val astUnliftable: Unliftable[Ast] = Unliftable[Ast] {
    case queryUnliftable(ast) => ast
    case actionUnliftable(ast) => ast
    case valueUnliftable(ast) => ast
    case identUnliftable(ast) => ast
    case orderingUnliftable(ast) => ast
    case q"$pack.Property.apply(${ a: Ast }, ${ b: String })" => Property(a, b)
    case q"$pack.Function.apply(${ a: List[Ident] }, ${ b: Ast })" => Function(a, b)
    case q"$pack.FunctionApply.apply(${ a: Ast }, ${ b: List[Ast] })" => FunctionApply(a, b)
    case q"$pack.BinaryOperation.apply(${ a: Ast }, ${ b: BinaryOperator }, ${ c: Ast })" => BinaryOperation(a, b, c)
    case q"$pack.UnaryOperation.apply(${ a: UnaryOperator }, ${ b: Ast })" => UnaryOperation(a, b)
    case q"$pack.Aggregation.apply(${ a: AggregationOperator }, ${ b: Ast })" => Aggregation(a, b)
    case q"$pack.Infix.apply(${ a: List[String] }, ${ b: List[Ast] })" => Infix(a, b)
    case q"$pack.OptionOperation.apply(${ a: OptionOperationType }, ${ b: Ast }, ${ c: Ident }, ${ d: Ast })" => OptionOperation(a, b, c, d)
    case q"$pack.If.apply(${ a: Ast }, ${ b: Ast }, ${ c: Ast })" => If(a, b, c)
    case q"$tree.ast" => Dynamic(tree)
    case q"$pack.RuntimeBinding.apply(${ a: String })" => RuntimeBinding(a)
  }

  implicit val optionOperationTypeUnliftable: Unliftable[OptionOperationType] = Unliftable[OptionOperationType] {
    case q"$pack.OptionMap"    => OptionMap
    case q"$pack.OptionForall" => OptionForall
    case q"$pack.OptionExists" => OptionExists
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
    case q"${ entityUnliftable(entity) }" => entity
    case q"$pack.Filter.apply(${ a: Ast }, ${ b: Ident }, ${ c: Ast })" => Filter(a, b, c)
    case q"$pack.Map.apply(${ a: Ast }, ${ b: Ident }, ${ c: Ast })" => Map(a, b, c)
    case q"$pack.FlatMap.apply(${ a: Ast }, ${ b: Ident }, ${ c: Ast })" => FlatMap(a, b, c)
    case q"$pack.SortBy.apply(${ a: Ast }, ${ b: Ident }, ${ c: Ast }, ${ d: Ast })" => SortBy(a, b, c, d)
    case q"$pack.GroupBy.apply(${ a: Ast }, ${ b: Ident }, ${ c: Ast })" => GroupBy(a, b, c)
    case q"$pack.Take.apply(${ a: Ast }, ${ b: Ast })" => Take(a, b)
    case q"$pack.Drop.apply(${ a: Ast }, ${ b: Ast })" => Drop(a, b)
    case q"$pack.Union.apply(${ a: Ast }, ${ b: Ast })" => Union(a, b)
    case q"$pack.UnionAll.apply(${ a: Ast }, ${ b: Ast })" => UnionAll(a, b)
    case q"$pack.Join.apply(${ t: JoinType }, ${ a: Ast }, ${ b: Ast }, ${ iA: Ident }, ${ iB: Ident }, ${ on: Ast })" =>
      Join(t, a, b, iA, iB, on)

    case q"$pack.Distinct.apply(${ a: Ast })" => Distinct(a)
  }

  implicit val entityUnliftable: Unliftable[Entity] = Unliftable[Entity] {
    case q"$pack.SimpleEntity.apply(${ a: String })" => SimpleEntity(a)
    case q"$pack.ConfiguredEntity.apply(${ a: Ast }, ${ b: Option[String] }, ${ c: List[PropertyAlias] }, ${ d: Option[String] })" => ConfiguredEntity(a, b, c, d)
  }

  implicit val entityConfigUnliftable: Unliftable[EntityConfig] = Unliftable[EntityConfig] {
    case q"$pack.EntityConfig.apply(${ a: Option[String] }, ${ b: List[PropertyAlias] }, ${ c: Option[String] })" =>
      EntityConfig(a, b, c)
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
    case q"$pack.PropertyAlias.apply(${ a: String }, ${ b: String })" => PropertyAlias(a, b)
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
    case q"$pack.AssignedAction.apply(${ a: Ast }, ${ b: List[Assignment] })" => AssignedAction(a, b)
    case q"$pack.Update.apply(${ a: Ast })"                                   => Update(a)
    case q"$pack.Insert.apply(${ a: Ast })"                                   => Insert(a)
    case q"$pack.Delete.apply(${ a: Ast })"                                   => Delete(a)
  }

  implicit val assignmentUnliftable: Unliftable[Assignment] = Unliftable[Assignment] {
    case q"$pack.Assignment.apply(${ a: Ident }, ${ b: String }, ${ c: Ast })" => Assignment(a, b, c)
  }

  implicit val valueUnliftable: Unliftable[Value] = Unliftable[Value] {
    case q"$pack.NullValue" => NullValue
    case q"$pack.Constant.apply(${ Literal(c.universe.Constant(a)) })" => Constant(a)
    case q"$pack.Tuple.apply(${ a: List[Ast] })" => Tuple(a)
    case q"$pack.Collection.apply(${ a: List[Ast] })" => Collection(a)
  }
  implicit val identUnliftable: Unliftable[Ident] = Unliftable[Ident] {
    case q"$pack.Ident.apply(${ a: String })" => Ident(a)
  }

}
