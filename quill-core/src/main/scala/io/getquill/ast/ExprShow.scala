package io.getquill.ast

import io.getquill.util.Show._

object ExprShow {

  implicit val exprShow: Show[Expr] = new Show[Expr] {
    def show(e: Expr) =
      e match {
        case predicate: Predicate => predicate.show
        case ref: Ref             => ref.show
        case Subtract(a, b)       => s"${a.show} - ${b.show}"
        case Add(a, b)            => s"${a.show} + ${b.show}"
      }
  }

  implicit val predicateShow: Show[Predicate] = new Show[Predicate] {
    def show(e: Predicate) =
      e match {
        case Equals(a, b)             => s"${a.show} == ${b.show}"
        case And(a, b)                => s"${a.show} && ${b.show}"
        case GreaterThan(a, b)        => s"${a.show} > ${b.show}"
        case GreaterThanOrEqual(a, b) => s"${a.show} >= ${b.show}"
        case LessThan(a, b)           => s"${a.show} < ${b.show}"
        case LessThanOrEqual(a, b)    => s"${a.show} <= ${b.show}"
      }
  }

  implicit val refShow: Show[Ref] = new Show[Ref] {
    def show(e: Ref) =
      e match {
        case Property(ref, name) =>
          s"${ref.show}.$name"
        case ident: Ident =>
          ident.show
        case v: Value =>
          v.show
      }
  }

  implicit val valueShow: Show[Value] = new Show[Value] {
    def show(e: Value) =
      e match {
        case Constant(v: String) =>
          s""""$v""""
        case Constant(v) =>
          s"$v"
        case NullValue =>
          s"null"
        case Tuple(values) =>
          s"(${values.map(_.show).mkString(", ")})"
      }
  }

  implicit val identShow: Show[Ident] = new Show[Ident] {
    def show(e: Ident) = e.name
  }

}