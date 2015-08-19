package io.getquill.ast

import io.getquill.util.Show.Show
import io.getquill.util.Show.Shower
import io.getquill.util.Show.listShow

object AstShow {

  implicit val astShow: Show[Ast] = new Show[Ast] {
    def show(e: Ast) =
      e match {
        case ast: Query           => ast.show
        case ast: Ref             => ast.show
        case operation: Operation => operation.show
      }
  }

  implicit val queryShow: Show[Query] = new Show[Query] {
    def show(q: Query) =
      q match {

        case Table(name) =>
          s"queryable[$name]"

        case Filter(source, alias, body) =>
          s"${source.show}.filter(${alias.show} => ${body.show})"

        case Map(source, alias, body) =>
          s"${source.show}.map(${alias.show} => ${body.show})"

        case FlatMap(source, alias, body) =>
          s"${source.show}.flatMap(${alias.show} => ${body.show})"
      }
  }

  implicit val operationShow: Show[Operation] = new Show[Operation] {
    def show(e: Operation) =
      e match {
        case UnaryOperation(op: PrefixUnaryOperator, ast)  => s"${op.show}${ast.show}"
        case UnaryOperation(op: PostfixUnaryOperator, ast) => s"${ast.show}.${op.show}"
        case BinaryOperation(a, op, b)                      => s"${a.show} ${op.show} ${b.show}"
      }
  }

  implicit val prefixUnaryOperatorShow: Show[PrefixUnaryOperator] = new Show[PrefixUnaryOperator] {
    def show(o: PrefixUnaryOperator) =
      o match {
        case io.getquill.ast.`!` => "!"
      }
  }

  implicit val postfixUnaryOperatorShow: Show[PostfixUnaryOperator] = new Show[PostfixUnaryOperator] {
    def show(o: PostfixUnaryOperator) =
      o match {
        case io.getquill.ast.`isEmpty`  => "isEmpty"
        case io.getquill.ast.`nonEmpty` => "nonEmpty"
      }
  }

  implicit val binaryOperatorShow: Show[BinaryOperator] = new Show[BinaryOperator] {
    def show(o: BinaryOperator) =
      o match {
        case io.getquill.ast.`-`    => "-"
        case io.getquill.ast.`+`    => "+"
        case io.getquill.ast.`*`    => "*"
        case io.getquill.ast.`==`   => "=="
        case io.getquill.ast.`!=`   => "!="
        case io.getquill.ast.`&&`   => "&&"
        case io.getquill.ast.`||`   => "||"
        case io.getquill.ast.`>`    => ">"
        case io.getquill.ast.`>=`   => ">="
        case io.getquill.ast.`<`    => "<"
        case io.getquill.ast.`<=`   => "<="
        case io.getquill.ast.`/`    => "/"
        case io.getquill.ast.`%`    => "%"
        case io.getquill.ast.`like` => "like"
      }
  }

  implicit val refShow: Show[Ref] = new Show[Ref] {
    def show(e: Ref) =
      e match {
        case Property(ref, name) => s"${ref.show}.$name"
        case ident: Ident        => ident.show
        case v: Value            => v.show
      }
  }

  implicit val valueShow: Show[Value] = new Show[Value] {
    def show(e: Value) =
      e match {
        case Constant(v: String) => s""""$v""""
        case Constant(())        => s"{}"
        case Constant(v)         => s"$v"
        case NullValue           => s"null"
        case Tuple(values)       => s"(${values.show})"
      }
  }

  implicit val identShow: Show[Ident] = new Show[Ident] {
    def show(e: Ident) = e.name
  }

}