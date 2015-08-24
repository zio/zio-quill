package io.getquill.ast

import io.getquill.util.Show.Show
import io.getquill.util.Show.Shower
import io.getquill.util.Show.listShow
import io.getquill.ast

object AstShow {

  implicit val astShow: Show[Ast] = new Show[Ast] {
    def show(e: Ast) =
      e match {
        case ast: Query         => ast.show
        case ast: Function      => ast.show
        case ast: FunctionApply => ast.show
        case ast: Value         => ast.show
        case ast: Operation     => ast.show
        case ast: Action        => ast.show
        case ast: Ident         => ast.show
        case ast: Property      => ast.show
      }
  }

  implicit val queryShow: Show[Query] = new Show[Query] {
    def show(q: Query) =
      q match {

        case Entity(name) =>
          s"queryable[$name]"

        case Filter(source, alias, body) =>
          s"${source.show}.filter(${alias.show} => ${body.show})"

        case Map(source, alias, body) =>
          s"${source.show}.map(${alias.show} => ${body.show})"

        case FlatMap(source, alias, body) =>
          s"${source.show}.flatMap(${alias.show} => ${body.show})"
      }
  }

  implicit val functionShow: Show[Function] = new Show[Function] {
    def show(q: Function) =
      q match {
        case Function(params, body) => s"((${params.show}) => ${body.show})"
      }
  }

  implicit val functionpplyShow: Show[FunctionApply] = new Show[FunctionApply] {
    def show(q: FunctionApply) =
      q match {
        case FunctionApply(function, values) => s"${function.show}.apply(${values.show})"
      }
  }

  implicit val operationShow: Show[Operation] = new Show[Operation] {
    def show(e: Operation) =
      e match {
        case UnaryOperation(op: PrefixUnaryOperator, ast)  => s"${op.show}(${ast.show})"
        case UnaryOperation(op: PostfixUnaryOperator, ast) => s"${ast.show}.${op.show}"
        case BinaryOperation(a, op, b)                     => s"${a.show} ${op.show} ${b.show}"
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
        case ast.`-`  => "-"
        case ast.`+`  => "+"
        case ast.`*`  => "*"
        case ast.`==` => "=="
        case ast.`!=` => "!="
        case ast.`&&` => "&&"
        case ast.`||` => "||"
        case ast.`>`  => ">"
        case ast.`>=` => ">="
        case ast.`<`  => "<"
        case ast.`<=` => "<="
        case ast.`/`  => "/"
        case ast.`%`  => "%"
      }
  }

  implicit val propertyShow: Show[Property] = new Show[Property] {
    def show(e: Property) =
      e match {
        case Property(ref, name) => s"${ref.show}.$name"
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

  implicit val actionShow: Show[Action] = new Show[Action] {
    def show(e: Action) =
      e match {
        case Update(query, assignments) => s"${query.show}.update(${assignments.show})"
        case Insert(query, assignments) => s"${query.show}.insert(${assignments.show})"
        case Delete(query)              => s"${query.show}.delete"
      }
  }

  implicit val assignmentShow: Show[Assignment] = new Show[Assignment] {
    def show(e: Assignment) =
      e match {
        case Assignment(property, value) => s"_.$property -> ${value.show}"
      }
  }

}
