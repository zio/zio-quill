package io.getquill.norm

import io.getquill.Spec
import io.getquill.ast._
import io.getquill.testContext._
import io.getquill.ast.NumericOperator

class FlattenOptionOperationSpec extends Spec {

  "transforms option operations into simple properties" - {
    "getOrElse" in {
      val q = quote {
        (o: Option[Int]) => o.getOrElse(1)
      }
      FlattenOptionOperation(q.ast.body: Ast) mustEqual
        If(BinaryOperation(Ident("o"), EqualityOperator.`!=`, NullValue), Ident("o"), Constant(1))
    }
    "flatten" in {
      val q = quote {
        (o: Option[Option[Int]]) => o.flatten.map(i => i + 1)
      }
      FlattenOptionOperation(q.ast.body: Ast) mustEqual
        BinaryOperation(Ident("o"), NumericOperator.`+`, Constant(1))
    }
    "flatMap" in {
      val q = quote {
        (o: Option[Option[Int]]) => o.flatMap(i => i.map(j => j + 1))
      }
      FlattenOptionOperation(q.ast.body: Ast) mustEqual
        BinaryOperation(Ident("o"), NumericOperator.`+`, Constant(1))
    }
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
    "map + forall" in {
      val q = quote {
        (o: Option[TestEntity]) => o.map(_.i).forall(i => i != 1)
      }
      FlattenOptionOperation(q.ast.body: Ast) mustEqual
        BinaryOperation(
          BinaryOperation(Property(Ident("o"), "i"), EqualityOperator.`==`, NullValue),
          BooleanOperator.`||`,
          BinaryOperation(Property(Ident("o"), "i"), EqualityOperator.`!=`, Constant(1))
        )
    }
    "map + forall + binop" in {
      val q = quote {
        (o: Option[TestEntity]) => o.map(_.i).forall(i => i != 1) && true
      }
      FlattenOptionOperation(q.ast.body: Ast) mustEqual
        BinaryOperation(
          BinaryOperation(
            BinaryOperation(Property(Ident("o"), "i"), EqualityOperator.`==`, NullValue),
            BooleanOperator.`||`,
            BinaryOperation(Property(Ident("o"), "i"), EqualityOperator.`!=`, Constant(1))
          ),
          BooleanOperator.`&&`,
          Constant(true)
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
