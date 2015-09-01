package io.getquill.ast

import scala.language.reflectiveCalls

import io.getquill.Spec

class StatelessTransformerSpec extends Spec {

  case class Subject(replace: (Ast, Ast)*) extends StatelessTransformer {
    override def apply(e: Ast): Ast =
      replace.toMap.getOrElse(e, super.apply(e))
  }

  "transforms asts" - {
    "query" - {
      "entity" in {
        val ast: Ast = Entity("a")
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
        val ast: Ast = SortBy(Ident("a"), Ident("b"), Ident("c"))
        Subject(Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) mustEqual
          SortBy(Ident("a'"), Ident("b"), Ident("c'"))
      }
      "reverse" in {
        val ast: Ast = Reverse(SortBy(Ident("a"), Ident("b"), Ident("c")))
        Subject(Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) mustEqual
          Reverse(SortBy(Ident("a'"), Ident("b"), Ident("c'")))
      }
    }

    "operation" - {
      "unary" in {
        val ast: Ast = UnaryOperation(io.getquill.ast.`!`, Ident("a"))
        Subject(Ident("a") -> Ident("a'"))(ast) mustEqual
          UnaryOperation(io.getquill.ast.`!`, Ident("a'"))
      }
      "binary" in {
        val ast: Ast = BinaryOperation(Ident("a"), io.getquill.ast.`&&`, Ident("b"))
        Subject(Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"))(ast) mustEqual
          BinaryOperation(Ident("a'"), io.getquill.ast.`&&`, Ident("b'"))
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
        val ast: Ast = Update(Ident("a"), List(Assignment("b", Ident("c"))))
        Subject(Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) mustEqual
          Update(Ident("a'"), List(Assignment("b", Ident("c'"))))
      }
      "insert" in {
        val ast: Ast = Insert(Ident("a"), List(Assignment("b", Ident("c"))))
        Subject(Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) mustEqual
          Insert(Ident("a'"), List(Assignment("b", Ident("c'"))))
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

    "function apply" in {
      val ast: Ast = FunctionApply(Ident("a"), List(Ident("b"), Ident("c")))
      Subject(Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) mustEqual
        FunctionApply(Ident("a'"), List(Ident("b'"), Ident("c'")))
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
  }
}
