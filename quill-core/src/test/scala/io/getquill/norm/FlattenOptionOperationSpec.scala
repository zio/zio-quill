package io.getquill.norm

import io.getquill.Spec
import io.getquill.ast._
import io.getquill.testContext._
import io.getquill.ast.NumericOperator
import io.getquill.ast.Implicits._
import io.getquill.norm.ConcatBehavior.{ AnsiConcat, NonAnsiConcat }
import io.getquill.MoreAstOps._

class FlattenOptionOperationSpec extends Spec {

  def o = Ident("o")
  def c1 = Constant(1)
  def cFoo = Constant("foo")
  def cBar = Constant("bar")
  def cValue = Constant("value")

  "transforms option operations into simple properties" - {
    case class Row(id: Int, value: String)

    "getOrElse" in {
      val q = quote {
        (o: Option[Int]) => o.getOrElse(1)
      }
      new FlattenOptionOperation(AnsiConcat)(q.ast.body: Ast) mustEqual
        If(BinaryOperation(Ident("o"), EqualityOperator.`!=`, NullValue), Ident("o"), Constant(1))
    }
    "flatten" - {
      "regular operation" in {
        val q = quote {
          (o: Option[Option[Int]]) => o.flatten.map(i => i + 1)
        }
        new FlattenOptionOperation(AnsiConcat)(q.ast.body: Ast) mustEqual
          o +++ c1
        new FlattenOptionOperation(NonAnsiConcat)(q.ast.body: Ast) mustEqual
          o +++ c1
      }
      "possible-fallthrough operation" in {
        val q = quote {
          (o: Option[Option[String]]) => o.flatten.map(s => s + "foo")
        }
        new FlattenOptionOperation(AnsiConcat)(q.ast.body: Ast) mustEqual
          o +++ Constant("foo")
        new FlattenOptionOperation(NonAnsiConcat)(q.ast.body: Ast) mustEqual
          IfExistElseNull(o, o +++ cFoo)
      }
      "never-fallthrough operation" in {
        val q = quote {
          (o: Option[Option[String]]) => o.flatten.map(s => if (s == "value") "foo" else "bar")
        }
        new FlattenOptionOperation(AnsiConcat)(q.ast.body: Ast) mustEqual
          IfExist(o, If(o +==+ cValue, cFoo, cBar), NullValue)
        new FlattenOptionOperation(NonAnsiConcat)(q.ast.body: Ast) mustEqual
          IfExist(o, If(o +==+ cValue, cFoo, cBar), NullValue)
      }
    }
    "flatMap" - {
      "regular operation" in {
        val q = quote {
          (o: Option[Option[Int]]) => o.flatMap(i => i.map(j => j + 1))
        }
        new FlattenOptionOperation(AnsiConcat)(q.ast.body: Ast) mustEqual
          o +++ c1
        new FlattenOptionOperation(NonAnsiConcat)(q.ast.body: Ast) mustEqual
          o +++ c1
      }
      "possible-fallthrough operation" in {
        val q = quote {
          (o: Option[Option[String]]) => o.flatMap(s => s.map(j => j + "foo"))
        }
        new FlattenOptionOperation(AnsiConcat)(q.ast.body: Ast) mustEqual
          o +++ cFoo
        new FlattenOptionOperation(NonAnsiConcat)(q.ast.body: Ast) mustEqual
          IfExistElseNull(o, IfExistElseNull(o, o +++ cFoo))
      }
      "never-fallthrough operation" in {
        val q = quote {
          (o: Option[Option[String]]) => o.flatMap(s => s.map(j => if (j == "value") "foo" else "bar"))
        }
        new FlattenOptionOperation(AnsiConcat)(q.ast.body: Ast) mustEqual
          IfExist(o, IfExist(o, If(o +==+ cValue, cFoo, cBar), NullValue), NullValue)
        new FlattenOptionOperation(NonAnsiConcat)(q.ast.body: Ast) mustEqual
          IfExist(o, IfExist(o, If(o +==+ cValue, cFoo, cBar), NullValue), NullValue)
      }
    }
    "flatMap row" in {
      val q = quote {
        (o: Option[Option[Row]]) => o.flatMap(i => i.map(j => j))
      }
      new FlattenOptionOperation(AnsiConcat)(q.ast.body: Ast) mustEqual o
    }
    "map" - {
      "regular operation" in {
        val q = quote {
          (o: Option[Int]) => o.map(i => i + 1)
        }
        new FlattenOptionOperation(AnsiConcat)(q.ast.body: Ast) mustEqual
          o +++ c1
        new FlattenOptionOperation(NonAnsiConcat)(q.ast.body: Ast) mustEqual
          o +++ c1
      }
      "possible-fallthrough operation" in {
        val q = quote {
          (o: Option[String]) => o.map(s => s + "foo")
        }
        new FlattenOptionOperation(AnsiConcat)(q.ast.body: Ast) mustEqual
          o +++ Constant("foo")
        new FlattenOptionOperation(NonAnsiConcat)(q.ast.body: Ast) mustEqual
          IfExistElseNull(o, o +++ cFoo)
      }
      "never-fallthrough operation" in {
        val q = quote {
          (o: Option[String]) => o.map(s => if (s == "value") "foo" else "bar")
        }
        new FlattenOptionOperation(AnsiConcat)(q.ast.body: Ast) mustEqual
          IfExist(o, If(o +==+ cValue, cFoo, cBar), NullValue)
        new FlattenOptionOperation(NonAnsiConcat)(q.ast.body: Ast) mustEqual
          IfExist(o, If(o +==+ cValue, cFoo, cBar), NullValue)
      }
    }
    "map row" in {
      val q = quote {
        (o: Option[Row]) => o.map(i => i)
      }
      new FlattenOptionOperation(AnsiConcat)(q.ast.body: Ast) mustEqual o
    }
    "map + getOrElse(true)" in {
      val q = quote {
        (o: Option[Int]) => o.map(_ < 1).getOrElse(true)
      }
      new FlattenOptionOperation(AnsiConcat)(q.ast.body: Ast) mustEqual
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
      new FlattenOptionOperation(AnsiConcat)(q.ast.body: Ast) mustEqual
        BinaryOperation(
          BinaryOperation(Ident("o"), NumericOperator.`<`, Constant(1)),
          BooleanOperator.`||`,
          OptionNonEmpty(Ident("o"))
        )
    }
    "forall" - {
      "regular operation" in {
        val q = quote {
          (o: Option[Int]) => o.forall(i => i != 1)
        }
        new FlattenOptionOperation(AnsiConcat)(q.ast.body: Ast) mustEqual
          (IsNullCheck(o) +||+ (o +!=+ c1))
        new FlattenOptionOperation(NonAnsiConcat)(q.ast.body: Ast) mustEqual
          (IsNullCheck(o) +||+ (o +!=+ c1))
      }
      "possible-fallthrough operation" in {
        val q = quote {
          (o: Option[String]) => o.forall(s => s + "foo" == "bar")
        }
        new FlattenOptionOperation(AnsiConcat)(q.ast.body: Ast) mustEqual
          (IsNullCheck(o) +||+ ((o +++ cFoo) +==+ cBar))
        new FlattenOptionOperation(NonAnsiConcat)(q.ast.body: Ast) mustEqual
          (IsNullCheck(o) +||+ (IsNotNullCheck(o) +&&+ ((o +++ cFoo) +==+ cBar)))
      }
      "never-fallthrough operation" in {
        val q = quote {
          (o: Option[String]) => o.forall(s => if (s + "foo" == "bar") true else false)
        }
        new FlattenOptionOperation(AnsiConcat)(q.ast.body: Ast) mustEqual
          (IsNullCheck(o) +||+ (IsNotNullCheck(o) +&&+ If((o +++ cFoo) +==+ cBar, Constant(true), Constant(false))))
        new FlattenOptionOperation(NonAnsiConcat)(q.ast.body: Ast) mustEqual
          (IsNullCheck(o) +||+ (IsNotNullCheck(o) +&&+ If((o +++ cFoo) +==+ cBar, Constant(true), Constant(false))))
      }
    }
    "map + forall + binop" - {
      "regular operation" in {
        val q = quote {
          (o: Option[TestEntity]) => o.map(_.i).forall(i => i != 1) && true
        }
        new FlattenOptionOperation(AnsiConcat)(q.ast.body: Ast) mustEqual
          ((IsNullCheck(Property(o, "i")) +||+ ((Property(o, "i") +!=+ c1))) +&&+ Constant(true))
      }
      "possible-fallthrough operation" in {
        val q = quote {
          (o: Option[TestEntity2]) => o.map(_.s).forall(s => s + "foo" == "bar") && true
        }
        new FlattenOptionOperation(AnsiConcat)(q.ast.body: Ast) mustEqual
          ((IsNullCheck(Property(o, "s")) +||+ ((Property(o, "s") +++ cFoo) +==+ cBar) +&&+ Constant(true)))
        new FlattenOptionOperation(NonAnsiConcat)(q.ast.body: Ast) mustEqual
          ((IsNullCheck(Property(o, "s")) +||+ (IsNotNullCheck(Property(o, "s")) +&&+ ((Property(o, "s") +++ cFoo) +==+ cBar))) +&&+ Constant(true))
      }
    }
    "exists" - {
      "regular operation" in {
        val q = quote {
          (o: Option[Int]) => o.exists(i => i > 1)
        }
        new FlattenOptionOperation(AnsiConcat)(q.ast.body: Ast) mustEqual
          (o +>+ c1)
        new FlattenOptionOperation(NonAnsiConcat)(q.ast.body: Ast) mustEqual
          (o +>+ c1)
      }
      "possible-fallthrough operation" in {
        val q = quote {
          (o: Option[String]) => o.exists(s => s + "foo" == "bar")
        }
        new FlattenOptionOperation(AnsiConcat)(q.ast.body: Ast) mustEqual
          ((o +++ cFoo) +==+ cBar)
        new FlattenOptionOperation(NonAnsiConcat)(q.ast.body: Ast) mustEqual
          (IsNotNullCheck(o) +&&+ ((o +++ cFoo) +==+ cBar))
      }
      "never-fallthrough operation" in {
        val q = quote {
          (o: Option[String]) => o.exists(s => if (s + "foo" == "bar") true else false)
        }
        new FlattenOptionOperation(AnsiConcat)(q.ast.body: Ast) mustEqual
          (IsNotNullCheck(o) +&&+ If((o +++ cFoo) +==+ cBar, Constant(true), Constant(false)))
        new FlattenOptionOperation(NonAnsiConcat)(q.ast.body: Ast) mustEqual
          (IsNotNullCheck(o) +&&+ If((o +++ cFoo) +==+ cBar, Constant(true), Constant(false)))
      }
    }
    "exists row" in {
      val q = quote {
        (o: Option[Row]) => o.exists(r => r.id != 1)
      }
      new FlattenOptionOperation(AnsiConcat)(q.ast.body: Ast) mustEqual (Property(o, "id") +!=+ c1)
    }
    "contains" in {
      val q = quote {
        (o: Option[Int]) => o.contains(1)
      }
      new FlattenOptionOperation(AnsiConcat)(q.ast.body: Ast) mustEqual
        BinaryOperation(Ident("o"), EqualityOperator.`==`, Constant(1))
    }
  }
}
