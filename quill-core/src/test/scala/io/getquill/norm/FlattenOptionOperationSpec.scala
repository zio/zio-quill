package io.getquill.norm

import io.getquill.Spec
import io.getquill.ast._
import io.getquill.testContext._
import io.getquill.ast.NumericOperator
import io.getquill.ast.Implicits._

class FlattenOptionOperationSpec extends Spec {

  implicit class AstOpsExt2(body: Ast) {
    def +++(other: Ast) = BinaryOperation(body, NumericOperator.`+`, other)
    def +>+(other: Ast) = BinaryOperation(body, NumericOperator.`>`, other)
    def +!=+(other: Ast) = BinaryOperation(body, EqualityOperator.`!=`, other)
  }

  def o = Ident("o")
  def c1 = Constant(1)

  "transforms option operations into simple properties" - {
    case class Row(id: Int, value: String)

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
        IfExistElseNull(o, o +++ c1)
    }
    "flatMap" in {
      val q = quote {
        (o: Option[Option[Int]]) => o.flatMap(i => i.map(j => j + 1))
      }
      FlattenOptionOperation(q.ast.body: Ast) mustEqual
        IfExistElseNull(o, IfExistElseNull(o, o +++ c1))
    }
    "flatMap row" in {
      val q = quote {
        (o: Option[Option[Row]]) => o.flatMap(i => i.map(j => j))
      }
      FlattenOptionOperation(q.ast.body: Ast) mustEqual o
    }
    "map" in {
      val q = quote {
        (o: Option[Int]) => o.map(i => i + 1)
      }
      FlattenOptionOperation(q.ast.body: Ast) mustEqual
        IfExistElseNull(o, o +++ c1)
    }
    "map row" in {
      val q = quote {
        (o: Option[Row]) => o.map(i => i)
      }
      FlattenOptionOperation(q.ast.body: Ast) mustEqual o
    }
    "map + getOrElse(true)" in {
      val q = quote {
        (o: Option[Int]) => o.map(_ < 1).getOrElse(true)
      }
      FlattenOptionOperation(q.ast.body: Ast) mustEqual
        BinaryOperation(
          BinaryOperation(Ident("o"), NumericOperator.`<`, Constant(1)),
          BooleanOperator.`||`,
          OptionIsEmpty(Ident("o"))
        )
    }
    "map + getOrElse(false)" in {
      val q = quote {
        (o: Option[Int]) => o.map(_ < 1).getOrElse(false)
      }
      FlattenOptionOperation(q.ast.body: Ast) mustEqual
        BinaryOperation(
          BinaryOperation(Ident("o"), NumericOperator.`<`, Constant(1)),
          BooleanOperator.`||`,
          OptionNonEmpty(Ident("o"))
        )
    }
    "forall" in {
      val q = quote {
        (o: Option[Int]) => o.forall(i => i != 1)
      }
      FlattenOptionOperation(q.ast.body: Ast) mustEqual
        (IsNullCheck(o) +||+ (IsNotNullCheck(o) +&&+ (o +!=+ c1)))
    }
    "map + forall + binop" in {
      val q = quote {
        (o: Option[TestEntity]) => o.map(_.i).forall(i => i != 1) && true
      }
      FlattenOptionOperation(q.ast.body: Ast) mustEqual
        ((IsNullCheck(Property(o, "i")) +||+ (IsNotNullCheck(Property(o, "i")) +&&+ (Property(o, "i") +!=+ c1))) +&&+ Constant(true))
    }
    "exists" in {
      val q = quote {
        (o: Option[Int]) => o.exists(i => i > 1)
      }
      FlattenOptionOperation(q.ast.body: Ast) mustEqual
        (IsNotNullCheck(o) +&&+ (o +>+ c1))
    }
    "exists row" in {
      val q = quote {
        (o: Option[Row]) => o.exists(r => r.id != 1)
      }
      FlattenOptionOperation(q.ast.body: Ast) mustEqual (Property(o, "id") +!=+ c1)
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
