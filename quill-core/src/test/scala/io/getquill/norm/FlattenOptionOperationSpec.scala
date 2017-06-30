package io.getquill.norm

import io.getquill.Spec
import io.getquill.ast._
import io.getquill.testContext._
import io.getquill.ast.NumericOperator

class FlattenOptionOperationSpec extends Spec {

  "transforms option operations into simple properties" - {
    "map" in {
      val q = quote {
        (o: Option[Int]) => o.map(i => i + 1)
      }
      FlattenOptionOperation(q.ast.body: Ast) mustEqual
        BinaryOperation(Ident("o"), NumericOperator.`+`, Constant(1))
    }
    "forall" in {
      val q = quote {
        (o: Option[Int]) => o.forall(i => i != 1)
      }
      FlattenOptionOperation(q.ast.body: Ast) mustEqual
        BinaryOperation(
          BinaryOperation(Ident("o"), EqualityOperator.`==`, NullValue),
          BooleanOperator.`||`,
          BinaryOperation(Ident("o"), EqualityOperator.`!=`, Constant(1))
        )
    }

    "exists" in {
      val q = quote {
        (o: Option[Int]) => o.exists(i => i > 1)
      }
      FlattenOptionOperation(q.ast.body: Ast) mustEqual
        BinaryOperation(Ident("o"), NumericOperator.`>`, Constant(1))
    }

    "contains" in {
      val q = quote {
        (o: Option[Int]) => o.contains(1)
      }
      FlattenOptionOperation(q.ast.body: Ast) mustEqual
        BinaryOperation(Ident("o"), EqualityOperator.`==`, Constant(1))
    }
  }
}
