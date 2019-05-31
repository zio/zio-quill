package io.getquill
import io.getquill.ast._

object MoreAstOps {
  implicit class AstOpsExt2(body: Ast) {
    def +++(other: Constant) =
      if (other.v.isInstanceOf[String])
        BinaryOperation(body, StringOperator.`+`, other)
      else
        BinaryOperation(body, NumericOperator.`+`, other)

    def +>+(other: Ast) = BinaryOperation(body, NumericOperator.`>`, other)
  }
}