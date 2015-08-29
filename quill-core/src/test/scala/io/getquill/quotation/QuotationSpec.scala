package io.getquill.quotation

import io.getquill.ast
import io.getquill.ast._
import io.getquill._
import io.getquill.Spec

class QuotationSpec extends Spec {

  "quotes and unquotes asts" - {
    "query" - {
      "entity" in {
        qr1.ast mustEqual Entity("TestEntity")
      }
      "filter" in {
        val q = quote {
          qr1.filter(t => t.s == "s")
        }
        quote(unquote(q)).ast mustEqual Filter(Entity("TestEntity"), Ident("t"), BinaryOperation(Property(Ident("t"), "s"), ast.`==`, Constant("s")))
      }
      "withFilter" in {
        val q = quote {
          qr1.withFilter(t => t.s == "s")
        }
        quote(unquote(q)).ast mustEqual Filter(Entity("TestEntity"), Ident("t"), BinaryOperation(Property(Ident("t"), "s"), ast.`==`, Constant("s")))
      }
      "map" in {
        val q = quote {
          qr1.map(t => t.s)
        }
        quote(unquote(q)).ast mustEqual Map(Entity("TestEntity"), Ident("t"), Property(Ident("t"), "s"))
      }
      "flatMap" in {
        val q = quote {
          qr1.flatMap(t => qr2)
        }
        quote(unquote(q)).ast mustEqual FlatMap(Entity("TestEntity"), Ident("t"), Entity("TestEntity2"))
      }
      "sortBy" in {
        val q = quote {
          qr1.sortBy(t => t.s)
        }
        quote(unquote(q)).ast mustEqual SortBy(Entity("TestEntity"), Ident("t"), Property(Ident("t"), "s"))
      }
    }
    "action" - {
      "update" in {
        val q = quote {
          qr1.update(_.s -> "s")
        }
        quote(unquote(q)).ast mustEqual Update(Entity("TestEntity"), List(Assignment("s", Constant("s"))))
      }
      "insert" in {
        val q = quote {
          qr1.insert(_.s -> "s")
        }
        quote(unquote(q)).ast mustEqual Insert(Entity("TestEntity"), List(Assignment("s", Constant("s"))))
      }
      "delete" in {
        val q = quote {
          qr1.delete
        }
        quote(unquote(q)).ast mustEqual Delete(Entity("TestEntity"))
      }
    }
    "value" - {
      "null" in {
        val q = quote(null)
        quote(unquote(q)).ast mustEqual NullValue
      }
      "constant" in {
        val q = quote(11L)
        quote(unquote(q)).ast mustEqual Constant(11L)
      }
      "tuple" in {
        val q = quote((1, "a"))
        quote(unquote(q)).ast mustEqual Tuple(List(Constant(1), Constant("a")))
      }
    }
    "ident" in {
      val q = quote {
        (s: String) => s
      }
      quote(unquote(q)).ast.body mustEqual Ident("s")
    }
    "property" in {
      val q = quote {
        qr1.map(t => t.s)
      }
      quote(unquote(q)).ast.body mustEqual Property(Ident("t"), "s")
    }
    "function" - {
      "anonymous function" in {
        val q = quote {
          (s: String) => s
        }
        quote(unquote(q)).ast mustEqual Function(List(Ident("s")), Ident("s"))
      }
      "anonymous class" in {
        val q = quote {
          new {
            def apply[T](q: Queryable[T]) = q
          }
        }
        quote(unquote(q)).ast mustEqual Function(List(Ident("q")), Ident("q"))
      }
    }
    "function apply" - {
      "local function" in {
        val f = quote {
          (s: String) => s
        }
        val q = quote {
          f("s")
        }
        quote(unquote(q)).ast mustEqual FunctionApply(f.ast, List(Constant("s")))
      }
      "function reference" in {
        val q = quote {
          (f: String => String) => f("a")
        }
        quote(unquote(q)).ast.body mustEqual FunctionApply(Ident("f"), List(Constant("a")))
      }
    }
    "binary operation" - {
      "-" in {
        val q = quote {
          (a: Int, b: Int) => a - b
        }
        quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), ast.`-`, Ident("b"))
      }
      "+" in {
        val q = quote {
          (a: Int, b: Int) => a + b
        }
        quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), ast.`+`, Ident("b"))
      }
      "*" in {
        val q = quote {
          (a: Int, b: Int) => a * b
        }
        quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), ast.`*`, Ident("b"))
      }
      "==" in {
        val q = quote {
          (a: Int, b: Int) => a == b
        }
        quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), ast.`==`, Ident("b"))
      }
      "!=" in {
        val q = quote {
          (a: Int, b: Int) => a != b
        }
        quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), ast.`!=`, Ident("b"))
      }
      "&&" in {
        val q = quote {
          (a: Boolean, b: Boolean) => a && b
        }
        quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), ast.`&&`, Ident("b"))
      }
      "||" in {
        val q = quote {
          (a: Boolean, b: Boolean) => a || b
        }
        quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), ast.`||`, Ident("b"))
      }
      ">" in {
        val q = quote {
          (a: Int, b: Int) => a > b
        }
        quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), ast.`>`, Ident("b"))
      }
      ">=" in {
        val q = quote {
          (a: Int, b: Int) => a >= b
        }
        quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), ast.`>=`, Ident("b"))
      }
      "<" in {
        val q = quote {
          (a: Int, b: Int) => a < b
        }
        quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), ast.`<`, Ident("b"))
      }
      "<=" in {
        val q = quote {
          (a: Int, b: Int) => a <= b
        }
        quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), ast.`<=`, Ident("b"))
      }
      "/" in {
        val q = quote {
          (a: Int, b: Int) => a / b
        }
        quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), ast.`/`, Ident("b"))
      }
      "%" in {
        val q = quote {
          (a: Int, b: Int) => a % b
        }
        quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), ast.`%`, Ident("b"))
      }
    }
    "unary operation" - {
      "!" in {
        val q = quote {
          (a: Boolean) => !a
        }
        quote(unquote(q)).ast.body mustEqual UnaryOperation(ast.`!`, Ident("a"))
      }
      "nonEmpty" in {
        val q = quote {
          qr1.nonEmpty
        }
        quote(unquote(q)).ast mustEqual UnaryOperation(ast.`nonEmpty`, Entity("TestEntity"))
      }
      "isEmpty" in {
        val q = quote {
          qr1.isEmpty
        }
        quote(unquote(q)).ast mustEqual UnaryOperation(ast.`isEmpty`, Entity("TestEntity"))
      }
    }
  }

  "reduces tuple matching locally" in {
    val q = quote {
      (t: (Int, Int)) =>
        t match {
          case (a, b) => a + b
        }
    }
    quote(unquote(q)).ast.body mustEqual BinaryOperation(Property(Ident("t"), "_1"), ast.`+`, Property(Ident("t"), "_2"))
  }

  "unquotes referenced quotations" - {
    val q = quote(1)
    val q2 = quote(q + 1)
    quote(unquote(q2)).ast mustEqual BinaryOperation(Constant(1), ast.`+`, Constant(1))
  }

  "ignores the ifrefutable call" - {
    val q = quote {
      qr1.map(t => (t.i, t.l))
    }
    quote {
      for {
        (a, b) <- q
      } yield {
        a + b
      }
    }
  }

  "fails if the tree is not valid" in {
    """quote("s".toUpperCase)""" mustNot compile
  }

  "fails if the quoted ast is not available" in {
    val q: Quoted[Int] = quote(1)
    "quote(unquote(q))" mustNot compile
  }

  "fails if the ast doesn't match the type" in {
    val q = new Quoted[Int => Int] {
      @QuotedAst(Constant(1))
      def quoted = ()
      override def ast = Constant(1)
    }
    "quote(q(1))" mustNot compile
  }
}
