package io.getquill.sql

import io.getquill.ast
import io.getquill.ast.BinaryOperation
import io.getquill.ast.BinaryOperator
import io.getquill.ast.Constant
import io.getquill.ast.Expr
import io.getquill.ast.Ident
import io.getquill.ast.NullValue
import io.getquill.ast.Property
import io.getquill.ast.Ref
import io.getquill.ast.Tuple
import io.getquill.ast.UnaryOperation
import io.getquill.ast.UnaryOperator
import io.getquill.ast.Value
import io.getquill.ast.Query
import io.getquill.util.Show.Show
import io.getquill.util.Show.Shower
import io.getquill.util.Show.listShow

object ExprShow {

  import SqlQueryShow._

  implicit def exprShow(implicit refShow: Show[Ref]): Show[Expr] =
    new Show[Expr] {
      def show(e: Expr) =
        e match {
          case query: Query                            => s"(${SqlQuery(query).show})"
          case ref: Ref                                => ref.show
          case UnaryOperation(op, expr)                => s"(${op.show} ${expr.show})"
          case BinaryOperation(a, ast.`==`, NullValue) => s"(${a.show} IS NULL)"
          case BinaryOperation(NullValue, ast.`==`, b) => s"(${b.show} IS NULL)"
          case BinaryOperation(a, op, b)               => s"(${a.show} ${op.show} ${b.show})"
        }
    }

  implicit val unaryOperatorShow: Show[UnaryOperator] = new Show[UnaryOperator] {
    def show(o: UnaryOperator) =
      o match {
        case io.getquill.ast.`!`        => "NOT"
        case io.getquill.ast.`isEmpty`  => "NOT EXISTS"
        case io.getquill.ast.`nonEmpty` => "NOT EXISTS"
      }
  }

  implicit val binaryOperatorShow: Show[BinaryOperator] = new Show[BinaryOperator] {
    def show(o: BinaryOperator) =
      o match {
        case ast.`-`    => "-"
        case ast.`+`    => "+"
        case ast.`*`    => "*"
        case ast.`==`   => "="
        case ast.`!=`   => "<>"
        case ast.`&&`   => "AND"
        case ast.`||`   => "OR"
        case ast.`>`    => ">"
        case ast.`>=`   => ">="
        case ast.`<`    => "<"
        case ast.`<=`   => "<="
        case ast.`/`    => "/"
        case ast.`%`    => "%"
        case ast.`like` => "like"
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
        case Constant(())        => s"1"
        case Constant(v)         => s"$v"
        case NullValue           => s"null"
        case Tuple(values)       => s"${values.show}"
      }
  }

  implicit val identShow: Show[Ident] = new Show[Ident] {
    def show(e: Ident) = e.name
  }
}
