package io.getquill.ast

trait StatelessTransformer {

  def apply(e: Ast): Ast =
    e match {
      case e: Query                    => apply(e)
      case e: Operation                => apply(e)
      case e: Action                   => apply(e)
      case e: Value                    => apply(e)

      case Function(params, body)      => Function(params, apply(body))
      case e: Ident                    => e
      case Property(a, name)           => Property(apply(a), name)
      case Infix(a, b)                 => Infix(a, b.map(apply))
      case OptionOperation(t, a, b, c) => OptionOperation(t, apply(a), b, apply(c))
      case If(a, b, c)                 => If(apply(a), apply(b), apply(c))

      case e: Dynamic                  => e
    }

  def apply(e: Query): Query =
    e match {
      case e: Entity          => e
      case Filter(a, b, c)    => Filter(apply(a), b, apply(c))
      case Map(a, b, c)       => Map(apply(a), b, apply(c))
      case FlatMap(a, b, c)   => FlatMap(apply(a), b, apply(c))
      case SortBy(a, b, c, d) => SortBy(apply(a), b, apply(c), d)
      case GroupBy(a, b, c)   => GroupBy(apply(a), b, apply(c))
      case Aggregation(o, a)  => Aggregation(o, apply(a))
      case Take(a, b)         => Take(apply(a), apply(b))
      case Drop(a, b)         => Drop(apply(a), apply(b))
      case Union(a, b)        => Union(apply(a), apply(b))
      case UnionAll(a, b)     => UnionAll(apply(a), apply(b))
      case Join(t, a, b, iA, iB, on) =>
        Join(t, apply(a), apply(b), iA, iB, apply(on))
    }

  def apply(e: Operation): Operation =
    e match {
      case UnaryOperation(o, a)            => UnaryOperation(o, apply(a))
      case BinaryOperation(a, b, c)        => BinaryOperation(apply(a), b, apply(c))
      case FunctionApply(function, values) => FunctionApply(apply(function), values.map(apply))
    }

  def apply(e: Value): Value =
    e match {
      case e: Constant   => e
      case NullValue     => NullValue
      case Tuple(values) => Tuple(values.map(apply))
      case Set(values)   => Set(values.map(apply))
    }

  def apply(e: Action): Action =
    e match {
      case AssignedAction(action, assignments) => AssignedAction(apply(action), assignments.map(apply))
      case Update(query)                       => Update(apply(query))
      case Insert(query)                       => Insert(apply(query))
      case Delete(query)                       => Delete(apply(query))
    }

  private def apply(e: Assignment): Assignment =
    e match {
      case Assignment(input, property, value) => Assignment(input, property, apply(value))
    }
}
