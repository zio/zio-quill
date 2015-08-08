package io.getquill.sql

import io.getquill.util.Show._
import io.getquill.ast._

object ExprShow {

  implicit def exprShow(implicit refShow: Show[Ref]): Show[Expr] =
    new Show[Expr] {
      def show(e: Expr) =
        e match {
          case ref: Ref                  => ref.show
          case UnaryOperation(op, expr)  => s"(${op.show} ${expr.show})"
          case BinaryOperation(a, op, b) => s"(${a.show} ${op.show} ${b.show})"
        }
    }

  implicit val unaryOperatorShow: Show[UnaryOperator] = new Show[UnaryOperator] {
    def show(o: UnaryOperator) =
      o match {
        case io.getquill.ast.`!` => "NOT"
      }
  }

  implicit val binaryOperatorShow: Show[BinaryOperator] = new Show[BinaryOperator] {
    def show(o: BinaryOperator) =
      o match {
        case io.getquill.ast.`-`    => "-"
        case io.getquill.ast.`+`    => "+"
        case io.getquill.ast.`==`   => "="
        case io.getquill.ast.`!=`   => "!="
        case io.getquill.ast.`&&`   => "AND"
        case io.getquill.ast.`||`   => "OR"
        case io.getquill.ast.`>`    => ">"
        case io.getquill.ast.`>=`   => ">="
        case io.getquill.ast.`<`    => "<"
        case io.getquill.ast.`<=`   => "<="
        case io.getquill.ast.`/`    => "/"
        case io.getquill.ast.`%`    => "%"
        case io.getquill.ast.`like` => "like"
      }
  }

  implicit def refShow(implicit valueShow: Show[Value], identShow: Show[Ident]): Show[Ref] =
    new Show[Ref] {
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
        case Constant(v: String) => s"'$v'"
        case Constant(v)         => s"$v"
        case NullValue           => s"null"
        case Tuple(values)       => s"${values.show}"
      }
  }

  implicit val identShow: Show[Ident] = new Show[Ident] {
    def show(e: Ident) = e.name
  }
}
