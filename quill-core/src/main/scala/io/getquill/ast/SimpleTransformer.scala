package io.getquill.ast

trait SimpleTransformer {

  def apply(e: Ast): Ast =
    e match {
      case e: Query     => apply(e)
      case e: Function  => apply(e)
      case e: Operation => apply(e)
      case e: Ref       => apply(e)
    }

  def apply(e: Query): Query =
    e match {
      case e: Table =>
        e
      case Filter(a, b, c)  => Filter(apply(a), b, apply(c))
      case Map(a, b, c)     => Map(apply(a), b, apply(c))
      case FlatMap(a, b, c) => FlatMap(apply(a), b, apply(c))
    }

  def apply(e: Function): Function =
    e match {
      case Function(params, body) => Function(params.map(apply), apply(body))
    }

  def apply(e: Operation): Operation =
    e match {
      case UnaryOperation(o, a)            => UnaryOperation(o, apply(a))
      case BinaryOperation(a, b, c)        => BinaryOperation(apply(a), b, apply(c))
      case FunctionApply(function, values) => FunctionApply(apply(function), values.map(apply))
    }

  def apply(e: Ref): Ref =
    e match {
      case Property(a, name) => Property(apply(a), name)
      case e: Ident          => apply(e)
      case e: Value          => apply(e)
    }

  def apply(e: Ident): Ident =
    e

  def apply(e: Value): Value =
    e match {
      case e: Constant   => e
      case NullValue     => NullValue
      case Tuple(values) => Tuple(values.map(apply))
    }
}
