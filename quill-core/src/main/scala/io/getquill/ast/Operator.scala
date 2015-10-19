package io.getquill.ast

sealed trait Operator

sealed trait UnaryOperator extends Operator

sealed trait PrefixUnaryOperator extends UnaryOperator

object `!` extends PrefixUnaryOperator

sealed trait PostfixUnaryOperator extends UnaryOperator

object `nonEmpty` extends PostfixUnaryOperator
object `isEmpty` extends PostfixUnaryOperator

sealed trait BinaryOperator extends Operator

object `-` extends BinaryOperator
object `+` extends BinaryOperator
object `*` extends BinaryOperator
object `==` extends BinaryOperator
object `!=` extends BinaryOperator
object `&&` extends BinaryOperator
object `||` extends BinaryOperator
object `>` extends BinaryOperator
object `>=` extends BinaryOperator
object `<` extends BinaryOperator
object `<=` extends BinaryOperator
object `/` extends BinaryOperator
object `%` extends BinaryOperator

sealed trait AggregationOperator extends Operator

object `min` extends AggregationOperator
object `max` extends AggregationOperator
object `avg` extends AggregationOperator
object `sum` extends AggregationOperator
object `size` extends AggregationOperator
