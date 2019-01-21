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
    "flatMap unchecked" in {
      val q = quote {
        (o: Option[Option[Int]]) => o.flatMap(i => i.map(j => j + 1))
      }
      FlattenOptionOperation(q.ast.body: Ast) mustEqual
        IfExistElseNull(o, IfExistElseNull(o, o +++ c1))
    }
    "map" in {
      val q = quote {
        (o: Option[Int]) => o.map(i => i + 1)
      }
      FlattenOptionOperation(q.ast.body: Ast) mustEqual
        IfExistElseNull(o, o +++ c1)
    }
    "map unchecked" in {
      val q = quote {
        (o: Option[Int]) => o.mapUnchecked(i => i + 1)
      }
      FlattenOptionOperation(q.ast.body: Ast) mustEqual
        o +++ c1
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
        (Empty(o) +||+ (Exist(o) +&&+ (o +!=+ c1)))
    }
    "forall unchecked" in {
      val q = quote {
        (o: Option[Int]) => o.forallUnchecked(i => i != 1)
      }
      FlattenOptionOperation(q.ast.body: Ast) mustEqual
        (Empty(o) +||+ (o +!=+ c1))
    }
    "map + forall" in {
      val q = quote {
        (o: Option[TestEntity]) => o.map(_.i).forall(i => i != 1)
      }
      FlattenOptionOperation(q.ast.body: Ast) mustEqual
        (Empty(Property(o, "i")) +||+ (Exist(Property(o, "i")) +&&+ (Property(o, "i") +!=+ c1)))
    }
    "map + forall unchecked" in {
      val q = quote {
        (o: Option[TestEntity]) => o.map(_.i).forall(i => i != 1)
      }
      FlattenOptionOperation(q.ast.body: Ast) mustEqual
        (Empty(Property(o, "i")) +||+ (Exist(Property(o, "i")) +&&+ (Property(o, "i") +!=+ c1)))
    }
    "map + forall + binop" in {
      val q = quote {
        (o: Option[TestEntity]) => o.map(_.i).forall(i => i != 1) && true
      }
      FlattenOptionOperation(q.ast.body: Ast) mustEqual
        ((Empty(Property(o, "i")) +||+ (Exist(Property(o, "i")) +&&+ (Property(o, "i") +!=+ c1))) +&&+ Constant(true))
    }
    "exists" in {
      val q = quote {
        (o: Option[Int]) => o.exists(i => i > 1)
      }
      FlattenOptionOperation(q.ast.body: Ast) mustEqual
        (Exist(o) +&&+ (o +>+ c1))
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
