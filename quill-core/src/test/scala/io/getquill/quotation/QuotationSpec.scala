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
        q.ast mustEqual Filter(Entity("TestEntity"), Ident("t"), BinaryOperation(Property(Ident("t"), "s"), ast.`==`, Constant("s")))
      }
      "withFilter" in {
        val q = quote {
          qr1.withFilter(t => t.s == "s")
        }
        q.ast mustEqual Filter(Entity("TestEntity"), Ident("t"), BinaryOperation(Property(Ident("t"), "s"), ast.`==`, Constant("s")))
      }
      "map" in {
        val q = quote {
          qr1.map(t => t.s)
        }
        q.ast mustEqual Map(Entity("TestEntity"), Ident("t"), Property(Ident("t"), "s"))
      }
      "flatMap" in {
        val q = quote {
          qr1.flatMap(t => qr2)
        }
        q.ast mustEqual FlatMap(Entity("TestEntity"), Ident("t"), Entity("TestEntity2"))
      }
    }
    "action" - {
      "update" in {
        val q = quote {
          qr1.update(_.s -> "s")
        }
        q.ast mustEqual Update(Entity("TestEntity"), List(Assignment("s", Constant("s"))))
      }
      "insert" in {
        val q = quote {
          qr1.insert(_.s -> "s")
        }
        q.ast mustEqual Insert(Entity("TestEntity"), List(Assignment("s", Constant("s"))))
      }
      "delete" in {
        val q = quote {
          qr1.delete
        }
        q.ast mustEqual Delete(Entity("TestEntity"))
      }
    }
    "value" - {
      "null" in {
        val q = quote(null)
        q.ast mustEqual NullValue
      }
      "constant" in {
        val q = quote(11L)
        q.ast mustEqual Constant(11L)
      }
      "tuple" in {
        val q = quote((1, "a"))
        q.ast mustEqual Tuple(List(Constant(1), Constant("a")))
      }
    }
    "ident" in {
      val q = quote {
        (s: String) => s
      }
      q.ast.body mustEqual Ident("s")
    }
    "property" in {
      val q = quote {
        qr1.map(t => t.s)
      }
      q.ast.body mustEqual Property(Ident("t"), "s")
    }
    "function" - {
      "anonymous function" in {
        val q = quote {
          (s: String) => s
        }
        q.ast mustEqual Function(List(Ident("s")), Ident("s"))
      }
      "anonymous class" in {
        val q = quote {
          new {
            def apply[T](q: Queryable[T]) = q
          }
        }
        q.ast mustEqual Function(List(Ident("q")), Ident("q"))
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
        q.ast mustEqual FunctionApply(f.ast, List(Constant("s")))
      }
      "function reference" in {
        val q = quote {
          (f: String => String) => f("a")
        }
        q.ast.body mustEqual FunctionApply(Ident("f"), List(Constant("a")))
      }
    }
    "binary operation" - {
      "-" in {
        val q = quote {
          (a: Int, b: Int) => a - b
        }
        q.ast.body mustEqual BinaryOperation(Ident("a"), ast.`-`, Ident("b"))
      }
      "+" in {
        val q = quote {
          (a: Int, b: Int) => a + b
        }
        q.ast.body mustEqual BinaryOperation(Ident("a"), ast.`+`, Ident("b"))
      }
      "*" in {
        val q = quote {
          (a: Int, b: Int) => a * b
        }
        q.ast.body mustEqual BinaryOperation(Ident("a"), ast.`*`, Ident("b"))
      }
      "==" in {
        val q = quote {
          (a: Int, b: Int) => a == b
        }
        q.ast.body mustEqual BinaryOperation(Ident("a"), ast.`==`, Ident("b"))
      }
      "!=" in {
        val q = quote {
          (a: Int, b: Int) => a != b
        }
        q.ast.body mustEqual BinaryOperation(Ident("a"), ast.`!=`, Ident("b"))
      }
      "&&" in {
        val q = quote {
          (a: Boolean, b: Boolean) => a && b
        }
        q.ast.body mustEqual BinaryOperation(Ident("a"), ast.`&&`, Ident("b"))
      }
      "||" in {
        val q = quote {
          (a: Boolean, b: Boolean) => a || b
        }
        q.ast.body mustEqual BinaryOperation(Ident("a"), ast.`||`, Ident("b"))
      }
      ">" in {
        val q = quote {
          (a: Int, b: Int) => a > b
        }
        q.ast.body mustEqual BinaryOperation(Ident("a"), ast.`>`, Ident("b"))
      }
      ">=" in {
        val q = quote {
          (a: Int, b: Int) => a >= b
        }
        q.ast.body mustEqual BinaryOperation(Ident("a"), ast.`>=`, Ident("b"))
      }
      "<" in {
        val q = quote {
          (a: Int, b: Int) => a < b
        }
        q.ast.body mustEqual BinaryOperation(Ident("a"), ast.`<`, Ident("b"))
      }
      "<=" in {
        val q = quote {
          (a: Int, b: Int) => a <= b
        }
        q.ast.body mustEqual BinaryOperation(Ident("a"), ast.`<=`, Ident("b"))
      }
      "/" in {
        val q = quote {
          (a: Int, b: Int) => a / b
        }
        q.ast.body mustEqual BinaryOperation(Ident("a"), ast.`/`, Ident("b"))
      }
      "%" in {
        val q = quote {
          (a: Int, b: Int) => a % b
        }
        q.ast.body mustEqual BinaryOperation(Ident("a"), ast.`%`, Ident("b"))
      }
    }
    "unary operation" - {
      "!" in {
        val q = quote {
          (a: Boolean) => !a
        }
        q.ast.body mustEqual UnaryOperation(ast.`!`, Ident("a"))
      }
      "nonEmpty" in {
        val q = quote {
          qr1.nonEmpty
        }
        q.ast mustEqual UnaryOperation(ast.`nonEmpty`, Entity("TestEntity"))
      }
      "isEmpty" in {
        val q = quote {
          qr1.isEmpty
        }
        q.ast mustEqual UnaryOperation(ast.`isEmpty`, Entity("TestEntity"))
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
    q.ast.body mustEqual BinaryOperation(Property(Ident("t"), "_1"), ast.`+`, Property(Ident("t"), "_2"))
  }

  "unquotes referenced quotations" - {
    val q = quote(1)
    val q2 = quote(q + 1)
    q2.ast mustEqual BinaryOperation(Constant(1), ast.`+`, Constant(1))
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
}
