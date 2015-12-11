package io.getquill.quotation

import io.getquill._
import io.getquill.ast.{ Query => _, _ }

class QuotationSpec extends Spec {

  "quotes and unquotes asts" - {

    "query" - {
      "entity" - {
        "without aliases" in {
          quote(unquote(qr1)).ast mustEqual Entity("TestEntity")
        }
        "with alias" in {
          val q = quote {
            query[TestEntity]("SomeAlias")
          }
          quote(unquote(q)).ast mustEqual Entity("TestEntity", Some("SomeAlias"))
        }
        "with property alias" in {
          val q = quote {
            query[TestEntity]("SomeAlias", _.s -> "theS", _.i -> "theI")
          }
          quote(unquote(q)).ast mustEqual Entity("TestEntity", Some("SomeAlias"), List(PropertyAlias("s", "theS"), PropertyAlias("i", "theI")))
        }
      }
      "filter" in {
        val q = quote {
          qr1.filter(t => t.s == "s")
        }
        quote(unquote(q)).ast mustEqual Filter(Entity("TestEntity"), Ident("t"), BinaryOperation(Property(Ident("t"), "s"), EqualityOperator.`==`, Constant("s")))
      }
      "withFilter" in {
        val q = quote {
          qr1.withFilter(t => t.s == "s")
        }
        quote(unquote(q)).ast mustEqual Filter(Entity("TestEntity"), Ident("t"), BinaryOperation(Property(Ident("t"), "s"), EqualityOperator.`==`, Constant("s")))
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
      "groupBy" in {
        val q = quote {
          qr1.groupBy(t => t.s)
        }
        quote(unquote(q)).ast mustEqual GroupBy(Entity("TestEntity"), Ident("t"), Property(Ident("t"), "s"))
      }
      "aggregation" - {
        "min" in {
          val q = quote {
            qr1.map(t => t.i).min
          }
          quote(unquote(q)).ast mustEqual Aggregation(AggregationOperator.`min`, Map(Entity("TestEntity"), Ident("t"), Property(Ident("t"), "i")))
        }
        "max" in {
          val q = quote {
            qr1.map(t => t.i).max
          }
          quote(unquote(q)).ast mustEqual Aggregation(AggregationOperator.`max`, Map(Entity("TestEntity"), Ident("t"), Property(Ident("t"), "i")))
        }
        "avg" in {
          val q = quote {
            qr1.map(t => t.i).avg
          }
          quote(unquote(q)).ast mustEqual Aggregation(AggregationOperator.`avg`, Map(Entity("TestEntity"), Ident("t"), Property(Ident("t"), "i")))
        }
        "sum" in {
          val q = quote {
            qr1.map(t => t.i).sum
          }
          quote(unquote(q)).ast mustEqual Aggregation(AggregationOperator.`sum`, Map(Entity("TestEntity"), Ident("t"), Property(Ident("t"), "i")))
        }
        "size" in {
          val q = quote {
            qr1.map(t => t.i).size
          }
          quote(unquote(q)).ast mustEqual Aggregation(AggregationOperator.`size`, Map(Entity("TestEntity"), Ident("t"), Property(Ident("t"), "i")))
        }
      }
      "reverse" in {
        val q = quote {
          qr1.sortBy(t => t.s).reverse
        }
        quote(unquote(q)).ast mustEqual Reverse(SortBy(Entity("TestEntity"), Ident("t"), Property(Ident("t"), "s")))
      }
      "take" in {
        val q = quote {
          qr1.take(10)
        }
        quote(unquote(q)).ast mustEqual Take(Entity("TestEntity"), Constant(10))
      }
      "drop" in {
        val q = quote {
          qr1.drop(10)
        }
        quote(unquote(q)).ast mustEqual Drop(Entity("TestEntity"), Constant(10))
      }
      "union" in {
        val q = quote {
          qr1.union(qr2)
        }
        quote(unquote(q)).ast mustEqual Union(Entity("TestEntity"), Entity("TestEntity2"))
      }
      "unionAll" - {
        "unionAll" in {
          val q = quote {
            qr1.union(qr2)
          }
          quote(unquote(q)).ast mustEqual Union(Entity("TestEntity"), Entity("TestEntity2"))
        }
        "++" in {
          val q = quote {
            qr1 ++ qr2
          }
          quote(unquote(q)).ast mustEqual UnionAll(Entity("TestEntity"), Entity("TestEntity2"))
        }
      }
      "outer join" - {

        def tree(t: OuterJoinType) =
          OuterJoin(t, Entity("TestEntity"), Entity("TestEntity2"), Ident("a"), Ident("b"), BinaryOperation(Property(Ident("a"), "s"), EqualityOperator.`==`, Property(Ident("b"), "s")))

        "left join" in {
          val q = quote {
            qr1.leftJoin(qr2).on((a, b) => a.s == b.s)
          }
          quote(unquote(q)).ast mustEqual tree(LeftJoin)
        }
        "right join" in {
          val q = quote {
            qr1.rightJoin(qr2).on((a, b) => a.s == b.s)
          }
          quote(unquote(q)).ast mustEqual tree(RightJoin)
        }
        "full join" in {
          val q = quote {
            qr1.fullJoin(qr2).on((a, b) => a.s == b.s)
          }
          quote(unquote(q)).ast mustEqual tree(FullJoin)
        }
        "fails if not followed by 'on'" in {
          """
          quote {
            qr1.fullJoin(qr2)
          }
          """ mustNot compile
        }
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
            def apply[T](q: Query[T]) = q
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
      "==" in {
        val q = quote {
          (a: Int, b: Int) => a == b
        }
        quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), EqualityOperator.`==`, Ident("b"))
      }
      "!=" in {
        val q = quote {
          (a: Int, b: Int) => a != b
        }
        quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), EqualityOperator.`!=`, Ident("b"))
      }
      "&&" in {
        val q = quote {
          (a: Boolean, b: Boolean) => a && b
        }
        quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), BooleanOperator.`&&`, Ident("b"))
      }
      "||" in {
        val q = quote {
          (a: Boolean, b: Boolean) => a || b
        }
        quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), BooleanOperator.`||`, Ident("b"))
      }
      "-" in {
        val q = quote {
          (a: Int, b: Int) => a - b
        }
        quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), NumericOperator.`-`, Ident("b"))
      }
      "+" - {
        "numeric" in {
          val q = quote {
            (a: Int, b: Int) => a + b
          }
          quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), NumericOperator.`+`, Ident("b"))
        }
        "string" in {
          val q = quote {
            (a: String, b: String) => a + b
          }
          quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), StringOperator.`+`, Ident("b"))
        }
      }
      "*" in {
        val q = quote {
          (a: Int, b: Int) => a * b
        }
        quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), NumericOperator.`*`, Ident("b"))
      }
      ">" in {
        val q = quote {
          (a: Int, b: Int) => a > b
        }
        quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), NumericOperator.`>`, Ident("b"))
      }
      ">=" in {
        val q = quote {
          (a: Int, b: Int) => a >= b
        }
        quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), NumericOperator.`>=`, Ident("b"))
      }
      "<" in {
        val q = quote {
          (a: Int, b: Int) => a < b
        }
        quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), NumericOperator.`<`, Ident("b"))
      }
      "<=" in {
        val q = quote {
          (a: Int, b: Int) => a <= b
        }
        quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), NumericOperator.`<=`, Ident("b"))
      }
      "/" in {
        val q = quote {
          (a: Int, b: Int) => a / b
        }
        quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), NumericOperator.`/`, Ident("b"))
      }
      "%" in {
        val q = quote {
          (a: Int, b: Int) => a % b
        }
        quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), NumericOperator.`%`, Ident("b"))
      }
    }
    "unary operation" - {
      "-" in {
        val q = quote {
          (a: Int) => -a
        }
        quote(unquote(q)).ast.body mustEqual UnaryOperation(NumericOperator.`-`, Ident("a"))
      }
      "!" in {
        val q = quote {
          (a: Boolean) => !a
        }
        quote(unquote(q)).ast.body mustEqual UnaryOperation(BooleanOperator.`!`, Ident("a"))
      }
      "nonEmpty" in {
        val q = quote {
          qr1.nonEmpty
        }
        quote(unquote(q)).ast mustEqual UnaryOperation(SetOperator.`nonEmpty`, Entity("TestEntity"))
      }
      "isEmpty" in {
        val q = quote {
          qr1.isEmpty
        }
        quote(unquote(q)).ast mustEqual UnaryOperation(SetOperator.`isEmpty`, Entity("TestEntity"))
      }
      "toUpperCase" in {
        val q = quote {
          qr1.map(t => t.s.toUpperCase)
        }
        quote(unquote(q)).ast mustEqual Map(Entity("TestEntity"), Ident("t"), UnaryOperation(StringOperator.`toUpperCase`, Property(Ident("t"), "s")))
      }
      "toLowerCase" in {
        val q = quote {
          qr1.map(t => t.s.toLowerCase)
        }
        quote(unquote(q)).ast mustEqual Map(Entity("TestEntity"), Ident("t"), UnaryOperation(StringOperator.`toLowerCase`, Property(Ident("t"), "s")))
      }
    }
    "infix" - {
      "without `as`" in {
        val q = quote {
          infix"true"
        }
        quote(unquote(q)).ast mustEqual Infix(List("true"), Nil)
      }
      "with `as`" in {
        val q = quote {
          infix"true".as[Boolean]
        }
        quote(unquote(q)).ast mustEqual Infix(List("true"), Nil)
      }
      "with params" in {
        val q = quote {
          (a: String, b: String) =>
            infix"$a || $b".as[String]
        }
        quote(unquote(q)).ast.body mustEqual Infix(List("", " || ", ""), List(Ident("a"), Ident("b")))
      }
    }
    "option operation" - {
      "map" in {
        val q = quote {
          (o: Option[Int]) => o.map(v => v)
        }
        quote(unquote(q)).ast.body mustEqual OptionOperation(OptionMap, Ident("o"), Ident("v"), Ident("v"))
      }
      "forall" in {
        val q = quote {
          (o: Option[Boolean]) => o.forall(v => v)
        }
        quote(unquote(q)).ast.body mustEqual OptionOperation(OptionForall, Ident("o"), Ident("v"), Ident("v"))
      }
    }
    "dynamic" - {
      "quotation" in {
        val filtered = quote {
          qr1.filter(t => t.i == 1)
        }
        def m(b: Boolean) =
          if (b)
            filtered
          else
            qr1
        val q1 = quote {
          unquote(m(true))
        }
        val q2 = quote {
          unquote(m(false))
        }
        quote(unquote(q1)).ast mustEqual filtered.ast
        quote(unquote(q2)).ast mustEqual qr1.ast
      }
      "value" in {
        val i = 1
        val l = quote {
          lift(i)
        }
        val q = quote {
          unquote(l)
        }
        quote(unquote(q)).ast mustEqual Constant(1)
      }
      "quoted dynamic" in {
        val i: Quoted[Int] = quote(1)
        val q: Quoted[Int] = quote(i + 1)
        quote(unquote(q)).ast mustEqual BinaryOperation(Constant(1), NumericOperator.`+`, Constant(1))
      }
      "abritrary tree" in {
        object test {
          def a = quote("a")
        }
        val q = quote {
          test.a
        }
        quote(unquote(q)).ast mustEqual Constant("a")
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
    quote(unquote(q)).ast.body mustEqual BinaryOperation(Property(Ident("t"), "_1"), NumericOperator.`+`, Property(Ident("t"), "_2"))
  }

  "unquotes referenced quotations" - {
    val q = quote(1)
    val q2 = quote(q + 1)
    quote(unquote(q2)).ast mustEqual BinaryOperation(Constant(1), NumericOperator.`+`, Constant(1))
  }

  "ignores the ifrefutable call" - {
    val q = quote {
      qr1.map(t => (t.i, t.l))
    }
    val n = quote {
      for {
        (a, b) <- q
      } yield {
        a + b
      }
    }
  }

  "fails if the tree is not valid" in {
    """quote("s".getBytes)""" mustNot compile
  }
}
