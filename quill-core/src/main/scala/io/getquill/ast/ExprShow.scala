package io.getquill.ast

import io.getquill.util.Show.Show
import io.getquill.util.Show.Shower
import io.getquill.util.Show.listShow

object ExprShow {

  implicit val exprShow: Show[Expr] = new Show[Expr] {
    def show(e: Expr) =
      e match {
        case ref: Ref             => ref.show
        case operation: Operation => operation.show
      }
  }

  implicit val operationShow: Show[Operation] = new Show[Operation] {
    def show(e: Operation) =
      e match {
        case UnaryOperation(op, expr)  => s"${op.show}${expr.show}"
        case BinaryOperation(a, op, b) => s"${a.show} ${op.show} ${b.show}"
      }
  }

  implicit val unaryOperatorShow: Show[UnaryOperator] = new Show[UnaryOperator] {
    def show(o: UnaryOperator) =
      o match {
        case io.getquill.ast.`!` => "!"
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
        case Constant(v)         => s"$v"
        case NullValue           => s"null"
        case Tuple(values)       => s"(${values.show})"
      }
  }

  implicit val identShow: Show[Ident] = new Show[Ident] {
    def show(e: Ident) = e.name
  }

}