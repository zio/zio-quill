package io.getquill.sql

import io.getquill.util.Show._
import io.getquill.ast._

object ExprShow {

  implicit def exprShow(implicit refShow: Show[Ref]): Show[Expr] =
    new Show[Expr] {
      def show(e: Expr) =
        e match {
          case ref: Ref                 => ref.show
          case Subtract(a, b)           => s"${a.show} - ${b.show}"
          case Division(a, b)           => s"${a.show} / ${b.show}"
          case Remainder(a, b)          => s"${a.show} % ${b.show}"
          case Add(a, b)                => s"${a.show} + ${b.show}"
          case Equals(a, b)             => s"${a.show} = ${b.show}"
          case And(a, b)                => s"${a.show} AND ${b.show}"
          case GreaterThan(a, b)        => s"${a.show} > ${b.show}"
          case GreaterThanOrEqual(a, b) => s"${a.show} >= ${b.show}"
          case LessThan(a, b)           => s"${a.show} < ${b.show}"
          case LessThanOrEqual(a, b)    => s"${a.show} <= ${b.show}"
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
