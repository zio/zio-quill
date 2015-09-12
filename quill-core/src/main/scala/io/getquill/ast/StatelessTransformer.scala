package io.getquill.ast

trait StatelessTransformer {

  def apply(e: Ast): Ast =
    e match {
      case e: Query                        => apply(e)
      case e: Operation                    => apply(e)
      case e: Action                       => apply(e)
      case e: Value                        => apply(e)

      case Function(params, body)          => Function(params, apply(body))
      case FunctionApply(function, values) => FunctionApply(apply(function), values.map(apply))
      case e: Ident                        => e
      case Property(a, name)               => Property(apply(a), name)
    }

  def apply(e: Query): Query =
    e match {
      case e: Entity        => e
      case Filter(a, b, c)  => Filter(apply(a), b, apply(c))
      case Map(a, b, c)     => Map(apply(a), b, apply(c))
      case FlatMap(a, b, c) => FlatMap(apply(a), b, apply(c))
      case SortBy(a, b, c)  => SortBy(apply(a), b, apply(c))
      case Reverse(a)       => Reverse(apply(a))
      case Take(a, b)       => Take(apply(a), apply(b))
    }

  def apply(e: Operation): Operation =
    e match {
      case UnaryOperation(o, a)     => UnaryOperation(o, apply(a))
      case BinaryOperation(a, b, c) => BinaryOperation(apply(a), b, apply(c))
    }

  def apply(e: Value): Value =
    e match {
      case e: Constant   => e
      case NullValue     => NullValue
      case Tuple(values) => Tuple(values.map(apply))
    }

  def apply(e: Action): Action =
    e match {
      case Update(query, assignments) => Update(apply(query), assignments.map(apply))
      case Insert(query, assignments) => Insert(apply(query), assignments.map(apply))
      case Delete(query)              => Delete(apply(query))
    }

  private def apply(e: Assignment): Assignment =
    e match {
      case Assignment(property, value) => Assignment(property, apply(value))
    }
}
