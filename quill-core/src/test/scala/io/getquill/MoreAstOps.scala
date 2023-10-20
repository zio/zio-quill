package io.getquill
import io.getquill.ast._

object MoreAstOps {
  implicit final class AstOpsExt2(private val body: Ast) extends AnyVal {
    def +++(other: Constant): BinaryOperation =
      if (other.v.isInstanceOf[String])
        BinaryOperation(body, StringOperator.`+`, other)
      else
        BinaryOperation(body, NumericOperator.`+`, other)

    def +>+(other: Ast): BinaryOperation = BinaryOperation(body, NumericOperator.`>`, other)
  }
}
