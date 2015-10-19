package io.getquill.ast

sealed trait Operator

sealed trait UnaryOperator extends Operator
sealed trait PrefixUnaryOperator extends UnaryOperator
sealed trait PostfixUnaryOperator extends UnaryOperator
sealed trait BinaryOperator extends Operator

sealed trait EqualityOperator extends Operator
object EqualityOperator {
  case object `==` extends EqualityOperator with BinaryOperator
  case object `!=` extends EqualityOperator with BinaryOperator
}

sealed trait BooleanOperator extends Operator
object BooleanOperator {
  case object `!` extends BooleanOperator with PrefixUnaryOperator
  case object `&&` extends BooleanOperator with BinaryOperator
  case object `||` extends BooleanOperator with BinaryOperator
}

sealed trait NumericOperator extends Operator
object NumericOperator {
  case object `-` extends NumericOperator with BinaryOperator
  case object `+` extends NumericOperator with BinaryOperator
  case object `*` extends NumericOperator with BinaryOperator
  case object `>` extends NumericOperator with BinaryOperator
  case object `>=` extends NumericOperator with BinaryOperator
  case object `<` extends NumericOperator with BinaryOperator
  case object `<=` extends NumericOperator with BinaryOperator
  case object `/` extends NumericOperator with BinaryOperator
  case object `%` extends NumericOperator with BinaryOperator
}

sealed trait SetOperator extends Operator
object SetOperator {
  case object `nonEmpty` extends SetOperator with PostfixUnaryOperator
  case object `isEmpty` extends SetOperator with PostfixUnaryOperator
}

sealed trait AggregationOperator extends Operator
object AggregationOperator {
  case object `min` extends AggregationOperator
  case object `max` extends AggregationOperator
  case object `avg` extends AggregationOperator
  case object `sum` extends AggregationOperator
  case object `size` extends AggregationOperator
}
