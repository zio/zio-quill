package io.getquill.ast

sealed trait Operator

sealed trait UnaryOperator extends Operator

sealed trait PrefixUnaryOperator extends UnaryOperator

object `!` extends PrefixUnaryOperator

sealed trait PostfixUnaryOperator extends UnaryOperator

object `nonEmpty` extends PostfixUnaryOperator
object `isEmpty` extends PostfixUnaryOperator
object `min` extends PostfixUnaryOperator
object `max` extends PostfixUnaryOperator
object `avg` extends PostfixUnaryOperator
object `sum` extends PostfixUnaryOperator
object `size` extends PostfixUnaryOperator

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
