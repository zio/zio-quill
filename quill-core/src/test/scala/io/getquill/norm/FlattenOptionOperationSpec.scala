package io.getquill.norm

import io.getquill.Spec
import io.getquill.ast._
import io.getquill.testContext._
import io.getquill.ast.NumericOperator

class FlattenOptionOperationSpec extends Spec {

  "transforms option operations into simple properties" in {
    val q = quote {
      (o: Option[Int]) => o.map(i => i + 1)
    }
    FlattenOptionOperation(q.ast.body: Ast) mustEqual
      BinaryOperation(Ident("o"), NumericOperator.`+`, Constant(1))
  }
}
