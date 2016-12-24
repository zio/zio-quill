package io.getquill.ast

import io.getquill.Spec

class StatelessTransformerSpec extends Spec {

  case class Subject(replace: (Ast, Ast)*) extends StatelessTransformer {
    override def apply(e: Ast): Ast =
      replace.toMap.getOrElse(e, super.apply(e))
  }

  "transforms asts" - {
    "query" - {
      "entity" in {
        val ast: Ast = Entity("a", Nil)
        Subject()(ast) mustEqual ast
      }
      "filter" in {
        val ast: Ast = Filter(Ident("a"), Ident("b"), Ident("c"))
        Subject(Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) mustEqual
          Filter(Ident("a'"), Ident("b"), Ident("c'"))
      }
      "map" in {
        val ast: Ast = Map(Ident("a"), Ident("b"), Ident("c"))
        Subject(Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) mustEqual
          Map(Ident("a'"), Ident("b"), Ident("c'"))
      }
      "flatMap" in {
        val ast: Ast = FlatMap(Ident("a"), Ident("b"), Ident("c"))
        Subject(Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) mustEqual
          FlatMap(Ident("a'"), Ident("b"), Ident("c'"))
      }
      "sortBy" in {
        val ast: Ast = SortBy(Ident("a"), Ident("b"), Ident("c"), AscNullsFirst)
        Subject(Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) mustEqual
          SortBy(Ident("a'"), Ident("b"), Ident("c'"), AscNullsFirst)
      }
      "groupBy" in {
        val ast: Ast = GroupBy(Ident("a"), Ident("b"), Ident("c"))
        Subject(Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) mustEqual
          GroupBy(Ident("a'"), Ident("b"), Ident("c'"))
      }
      "aggregation" in {
        val ast: Ast = Aggregation(AggregationOperator.`max`, Ident("a"))
        Subject(Ident("a") -> Ident("a'"))(ast) mustEqual
          Aggregation(AggregationOperator.`max`, Ident("a'"))
      }
      "take" in {
        val ast: Ast = Take(Ident("a"), Ident("b"))
        Subject(Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"))(ast) mustEqual
          Take(Ident("a'"), Ident("b'"))
      }
      "drop" in {
        val ast: Ast = Drop(Ident("a"), Ident("b"))
        Subject(Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"))(ast) mustEqual
          Drop(Ident("a'"), Ident("b'"))
      }
      "union" in {
        val ast: Ast = Union(Ident("a"), Ident("b"))
        Subject(Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"))(ast) mustEqual
          Union(Ident("a'"), Ident("b'"))
      }
      "unionAll" in {
        val ast: Ast = UnionAll(Ident("a"), Ident("b"))
        Subject(Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"))(ast) mustEqual
          UnionAll(Ident("a'"), Ident("b'"))
      }
      "outer join" in {
        val ast: Ast = Join(FullJoin, Ident("a"), Ident("b"), Ident("c"), Ident("d"), Ident("e"))
        Subject(Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("e") -> Ident("e'"))(ast) mustEqual
          Join(FullJoin, Ident("a'"), Ident("b'"), Ident("c"), Ident("d"), Ident("e'"))
      }

      "distinct" in {
        val ast: Ast = Distinct(Ident("a"))
        Subject(Ident("a") -> Ident("a'"))(ast) mustEqual Distinct(Ident("a'"))
      }
    }

    "operation" - {
      "unary" in {
        val ast: Ast = UnaryOperation(BooleanOperator.`!`, Ident("a"))
        Subject(Ident("a") -> Ident("a'"))(ast) mustEqual
          UnaryOperation(BooleanOperator.`!`, Ident("a'"))
      }
      "binary" in {
        val ast: Ast = BinaryOperation(Ident("a"), BooleanOperator.`&&`, Ident("b"))
        Subject(Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"))(ast) mustEqual
          BinaryOperation(Ident("a'"), BooleanOperator.`&&`, Ident("b'"))
      }
      "function apply" in {
        val ast: Ast = FunctionApply(Ident("a"), List(Ident("b"), Ident("c")))
        Subject(Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) mustEqual
          FunctionApply(Ident("a'"), List(Ident("b'"), Ident("c'")))
      }
    }

    "value" - {
      "constant" in {
        val ast: Ast = Constant("a")
        Subject()(ast) mustEqual ast
      }
      "null" in {
        val ast: Ast = NullValue
        Subject()(ast) mustEqual ast
      }
      "tuple" in {
        val ast: Ast = Tuple(List(Ident("a"), Ident("b"), Ident("c")))
        Subject(Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) mustEqual
          Tuple(List(Ident("a'"), Ident("b'"), Ident("c'")))
      }
    }

    "action" - {
      "update" in {
        val ast: Ast = Update(Ident("a"), List(Assignment(Ident("b"), Ident("c"), Ident("d"))))
        Subject(Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"), Ident("d") -> Ident("d'"))(ast) mustEqual
          Update(Ident("a'"), List(Assignment(Ident("b"), Ident("c'"), Ident("d'"))))
      }
      "insert" in {
        val ast: Ast = Insert(Ident("a"), List(Assignment(Ident("b"), Ident("c"), Ident("d"))))
        Subject(Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"), Ident("d") -> Ident("d'"))(ast) mustEqual
          Insert(Ident("a'"), List(Assignment(Ident("b"), Ident("c'"), Ident("d'"))))
      }
      "delete" in {
        val ast: Ast = Delete(Ident("a"))
        Subject(Ident("a") -> Ident("a'"))(ast) mustEqual
          Delete(Ident("a'"))
      }
    }

    "function" in {
      val ast: Ast = Function(List(Ident("a")), Ident("a"))
      Subject(Ident("a") -> Ident("b"))(ast) mustEqual
        Function(List(Ident("a")), Ident("b"))
    }

    "ident" in {
      val ast: Ast = Ident("a")
      Subject(Ident("a") -> Ident("a'"))(ast) mustEqual
        Ident("a'")
    }

    "property" in {
      val ast: Ast = Property(Ident("a"), "b")
      Subject(Ident("a") -> Ident("a'"))(ast) mustEqual
        Property(Ident("a'"), "b")
    }

    "infix" in {
      val ast: Ast = Infix(List("test"), List(Ident("a"), Ident("b")))
      Subject(Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"))(ast) mustEqual
        Infix(List("test"), List(Ident("a'"), Ident("b'")))
    }

    "option operation" - {
      "map" in {
        val ast: Ast = OptionMap(Ident("a"), Ident("b"), Ident("c"))
        Subject(Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) mustEqual
          OptionMap(Ident("a'"), Ident("b"), Ident("c'"))
      }
      "forall" in {
        val ast: Ast = OptionForall(Ident("a"), Ident("b"), Ident("c"))
        Subject(Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) mustEqual
          OptionForall(Ident("a'"), Ident("b"), Ident("c'"))
      }
      "exists" in {
        val ast: Ast = OptionExists(Ident("a"), Ident("b"), Ident("c"))
        Subject(Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) mustEqual
          OptionExists(Ident("a'"), Ident("b"), Ident("c'"))
      }
      "contains" in {
        val ast: Ast = OptionContains(Ident("a"), Ident("c"))
        Subject(Ident("a") -> Ident("a'"), Ident("c") -> Ident("c'"))(ast) mustEqual
          OptionContains(Ident("a'"), Ident("c'"))
      }
    }

    "if" in {
      val ast: Ast = If(Ident("a"), Ident("b"), Ident("c"))
      Subject(Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) mustEqual
        If(Ident("a'"), Ident("b'"), Ident("c'"))
    }

    "dynamic" in {
      val ast: Ast = Dynamic(Ident("a"))
      Subject(Ident("a") -> Ident("a'"))(ast) mustEqual ast
    }

    "block" in {
      val ast: Ast = Block(List(
        Val(Ident("a"), Entity("a", Nil)),
        Val(Ident("b"), Entity("b", Nil))
      ))
      Subject(Entity("a", Nil) -> Entity("b", Nil), Entity("b", Nil) -> Entity("c", Nil))(ast) mustEqual
        Block(List(
          Val(Ident("a"), Entity("b", Nil)),
          Val(Ident("b"), Entity("c", Nil))
        ))
    }

    "val" in {
      val ast: Ast = Val(Ident("a"), Entity("a", Nil))
      Subject(Entity("a", Nil) -> Entity("b", Nil))(ast) mustEqual
        Val(Ident("a"), Entity("b", Nil))
    }
  }
}
