package io.getquill.ast

sealed trait Operator

sealed trait UnaryOperator extends Operator
sealed trait PrefixUnaryOperator extends UnaryOperator
sealed trait PostfixUnaryOperator extends UnaryOperator
sealed trait BinaryOperator extends Operator

object EqualityOperator {
  case object `==` extends BinaryOperator
  case object `!=` extends BinaryOperator
}

object BooleanOperator {
  case object `!` extends PrefixUnaryOperator
  case object `&&` extends BinaryOperator
  case object `||` extends BinaryOperator
}

object StringOperator {
  case object `+` extends BinaryOperator
  case object `startsWith` extends BinaryOperator
  case object `split` extends BinaryOperator
  case object `toUpperCase` extends PostfixUnaryOperator
  case object `toLowerCase` extends PostfixUnaryOperator
  case object `toLong` extends PostfixUnaryOperator
  case object `toInt` extends PostfixUnaryOperator
}

object NumericOperator {
  case object `-` extends BinaryOperator with PrefixUnaryOperator
  case object `+` extends BinaryOperator
  case object `*` extends BinaryOperator
  case object `>` extends BinaryOperator
  case object `>=` extends BinaryOperator
  case object `<` extends BinaryOperator
  case object `<=` extends BinaryOperator
  case object `/` extends BinaryOperator
  case object `%` extends BinaryOperator
}

object SetOperator {
  case object `contains` extends BinaryOperator
  case object `nonEmpty` extends PostfixUnaryOperator
  case object `isEmpty` extends PostfixUnaryOperator
}

sealed trait AggregationOperator extends Operator

object AggregationOperator {
  case object `min` extends AggregationOperator
  case object `max` extends AggregationOperator
  case object `avg` extends AggregationOperator
  case object `sum` extends AggregationOperator
  case object `size` extends AggregationOperator
}
