package io.getquill.quotation

import scala.reflect.macros.whitebox.Context

import io.getquill._
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
    case q"$pack.Infix.apply(${ a: List[String] }, ${ b: List[Ast] })" => Infix(a, b)
  }

  implicit def listUnliftable[T](implicit u: Unliftable[T]): Unliftable[List[T]] = Unliftable[List[T]] {
    case q"$pack.Nil"                         => Nil
    case q"$pack.List.apply[..$t](..$values)" => values.map(u.unapply(_)).flatten
  }

  implicit val binaryOperatorUnliftable: Unliftable[BinaryOperator] = Unliftable[BinaryOperator] {
    case q"$pack.`-`"  => ast.`-`
    case q"$pack.`+`"  => ast.`+`
    case q"$pack.`*`"  => ast.`*`
    case q"$pack.`==`" => ast.`==`
    case q"$pack.`!=`" => ast.`!=`
    case q"$pack.`&&`" => ast.`&&`
    case q"$pack.`||`" => ast.`||`
    case q"$pack.`>`"  => ast.`>`
    case q"$pack.`>=`" => ast.`>=`
    case q"$pack.`<`"  => ast.`<`
    case q"$pack.`<=`" => ast.`<=`
    case q"$pack.`/`"  => ast.`/`
    case q"$pack.`%`"  => ast.`%`
  }

  implicit val unaryOperatorUnliftable: Unliftable[UnaryOperator] = Unliftable[UnaryOperator] {
    case q"$pack.`!`"        => ast.`!`
    case q"$pack.`nonEmpty`" => ast.`nonEmpty`
    case q"$pack.`isEmpty`"  => ast.`isEmpty`
  }

  implicit val queryUnliftable: Unliftable[Query] = Unliftable[Query] {
    case q"$pack.Entity.apply(${ name: String })"                        => Entity(name)
    case q"$pack.Filter.apply(${ a: Ast }, ${ b: Ident }, ${ c: Ast })"  => Filter(a, b, c)
    case q"$pack.Map.apply(${ a: Ast }, ${ b: Ident }, ${ c: Ast })"     => Map(a, b, c)
    case q"$pack.FlatMap.apply(${ a: Ast }, ${ b: Ident }, ${ c: Ast })" => FlatMap(a, b, c)
    case q"$pack.SortBy.apply(${ a: Ast }, ${ b: Ident }, ${ c: Ast })"  => SortBy(a, b, c)
    case q"$pack.Reverse.apply(${ a: Ast })"                             => Reverse(a)
    case q"$pack.Take.apply(${ a: Ast }, ${ b: Ast })"                   => Take(a, b)
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
