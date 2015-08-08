package io.getquill.ast

sealed trait Operator

sealed trait UnaryOperator extends Operator

object `!` extends UnaryOperator

sealed trait BinaryOperator extends Operator

object `-` extends BinaryOperator
object `+` extends BinaryOperator
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
object `like` extends BinaryOperator