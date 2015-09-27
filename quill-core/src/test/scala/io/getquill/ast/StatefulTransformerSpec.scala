package io.getquill.ast

import io.getquill._

class StatefulTransformerSpec extends Spec {

  case class Subject(state: List[Ast], replace: (Ast, Ast)*) extends StatefulTransformer[List[Ast]] {
    override def apply(e: Ast) = {
      replace.toMap.get(e) match {
        case Some(ast) => (ast, Subject(state :+ e, replace: _*))
        case None      => super.apply(e)
      }
    }
  }

  "transforms asts using a transformation state" - {
    "query" - {
      "entity" in {
        val ast: Ast = Entity("a")
        Subject(List())(ast) match {
          case (at, att) =>
            at mustEqual ast
            att.state mustEqual List()
        }
      }
      "filter" in {
        val ast: Ast = Filter(Ident("a"), Ident("b"), Ident("c"))
        Subject(List(), Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) match {
          case (at, att) =>
            at mustEqual Filter(Ident("a'"), Ident("b"), Ident("c'"))
            att.state mustEqual List(Ident("a"), Ident("c"))
        }

      }
      "map" in {
        val ast: Ast = Map(Ident("a"), Ident("b"), Ident("c"))
        Subject(List(), Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) match {
          case (at, att) =>
            at mustEqual Map(Ident("a'"), Ident("b"), Ident("c'"))
            att.state mustEqual List(Ident("a"), Ident("c"))
        }
      }
      "flatMap" in {
        val ast: Ast = FlatMap(Ident("a"), Ident("b"), Ident("c"))
        Subject(List(), Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) match {
          case (at, att) =>
            at mustEqual FlatMap(Ident("a'"), Ident("b"), Ident("c'"))
            att.state mustEqual List(Ident("a"), Ident("c"))
        }
      }
      "sortBy" in {
        val ast: Ast = SortBy(Ident("a"), Ident("b"), Ident("c"))
        Subject(List(), Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) match {
          case (at, att) =>
            at mustEqual SortBy(Ident("a'"), Ident("b"), Ident("c'"))
            att.state mustEqual List(Ident("a"), Ident("c"))
        }
      }
      "reverse" in {
        val ast: Ast = Reverse(SortBy(Ident("a"), Ident("b"), Ident("c")))
        Subject(List(), Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) match {
          case (at, att) =>
            at mustEqual Reverse(SortBy(Ident("a'"), Ident("b"), Ident("c'")))
            att.state mustEqual List(Ident("a"), Ident("c"))
        }
      }
      "take" in {
        val ast: Ast = Take(Ident("a"), Ident("b"))
        Subject(List(), Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"))(ast) match {
          case (at, att) =>
            at mustEqual Take(Ident("a'"), Ident("b'"))
            att.state mustEqual List(Ident("a"), Ident("b"))
        }
      }
      "drop" in {
        val ast: Ast = Drop(Ident("a"), Ident("b"))
        Subject(List(), Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"))(ast) match {
          case (at, att) =>
            at mustEqual Drop(Ident("a'"), Ident("b'"))
            att.state mustEqual List(Ident("a"), Ident("b"))
        }
      }
      "union" in {
        val ast: Ast = Union(Ident("a"), Ident("b"))
        Subject(List(), Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"))(ast) match {
          case (at, att) =>
            at mustEqual Union(Ident("a'"), Ident("b'"))
            att.state mustEqual List(Ident("a"), Ident("b"))
        }
      }
    }

    "operation" - {
      "unary" in {
        val ast: Ast = UnaryOperation(io.getquill.ast.`!`, Ident("a"))
        Subject(List(), Ident("a") -> Ident("a'"))(ast) match {
          case (at, att) =>
            at mustEqual UnaryOperation(io.getquill.ast.`!`, Ident("a'"))
            att.state mustEqual List(Ident("a"))
        }
      }
      "binary" in {
        val ast: Ast = BinaryOperation(Ident("a"), io.getquill.ast.`&&`, Ident("b"))
        Subject(List(), Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"))(ast) match {
          case (at, att) =>
            at mustEqual BinaryOperation(Ident("a'"), io.getquill.ast.`&&`, Ident("b'"))
            att.state mustEqual List(Ident("a"), Ident("b"))
        }
      }
    }

    "value" - {
      "constant" in {
        val ast: Ast = Constant("a")
        Subject(List())(ast) match {
          case (at, att) =>
            at mustEqual ast
            att.state mustEqual List()
        }
      }
      "null" in {
        val ast: Ast = NullValue
        Subject(List())(ast) match {
          case (at, att) =>
            at mustEqual ast
            att.state mustEqual List()
        }
      }
      "tuple" in {
        val ast: Ast = Tuple(List(Ident("a"), Ident("b"), Ident("c")))
        Subject(List(), Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) match {
          case (at, att) =>
            at mustEqual Tuple(List(Ident("a'"), Ident("b'"), Ident("c'")))
            att.state mustEqual List(Ident("a"), Ident("b"), Ident("c"))
        }
      }
    }

    "action" - {
      "update" in {
        val ast: Ast = Update(Ident("a"), List(Assignment("b", Ident("c"))))
        Subject(List(), Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) match {
          case (at, att) =>
            at mustEqual Update(Ident("a'"), List(Assignment("b", Ident("c'"))))
            att.state mustEqual List(Ident("a"), Ident("c"))
        }
      }
      "insert" in {
        val ast: Ast = Insert(Ident("a"), List(Assignment("b", Ident("c"))))
        Subject(List(), Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) match {
          case (at, att) =>
            at mustEqual Insert(Ident("a'"), List(Assignment("b", Ident("c'"))))
            att.state mustEqual List(Ident("a"), Ident("c"))
        }
      }
      "delete" in {
        val ast: Ast = Delete(Ident("a"))
        Subject(List(), Ident("a") -> Ident("a'"))(ast) match {
          case (at, att) =>
            at mustEqual Delete(Ident("a'"))
            att.state mustEqual List(Ident("a"))
        }
      }
    }

    "function" in {
      val ast: Ast = Function(List(Ident("a")), Ident("a"))
      Subject(List(), Ident("a") -> Ident("a'"))(ast) match {
        case (at, att) =>
          at mustEqual Function(List(Ident("a")), Ident("a'"))
          att.state mustEqual List(Ident("a"))
      }
    }

    "function apply" in {
      val ast: Ast = FunctionApply(Ident("a"), List(Ident("b"), Ident("c")))
      Subject(List(), Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) match {
        case (at, att) =>
          at mustEqual FunctionApply(Ident("a'"), List(Ident("b'"), Ident("c'")))
          att.state mustEqual List(Ident("a"), Ident("b"), Ident("c"))
      }
    }

    "ident" in {
      val ast: Ast = Ident("a")
      Subject(List(), Ident("a") -> Ident("a'"))(ast) match {
        case (at, att) =>
          at mustEqual Ident("a'")
          att.state mustEqual List(Ident("a"))
      }
    }

    "property" in {
      val ast: Ast = Property(Ident("a"), "b")
      Subject(List(), Ident("a") -> Ident("a'"))(ast) match {
        case (at, att) =>
          at mustEqual Property(Ident("a'"), "b")
          att.state mustEqual List(Ident("a"))
      }
    }

    "infix" in {
      val ast: Ast = Infix(List("test"), List(Ident("a")))
      Subject(List(), Ident("a") -> Ident("a'"))(ast) match {
        case (at, att) =>
          at mustEqual Infix(List("test"), List(Ident("a'")))
          att.state mustEqual List(Ident("a"))
      }
    }
  }
}
