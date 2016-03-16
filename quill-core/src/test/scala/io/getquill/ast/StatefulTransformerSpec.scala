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
        Subject(Nil)(ast) match {
          case (at, att) =>
            at mustEqual ast
            att.state mustEqual Nil
        }
      }
      "filter" in {
        val ast: Ast = Filter(Ident("a"), Ident("b"), Ident("c"))
        Subject(Nil, Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) match {
          case (at, att) =>
            at mustEqual Filter(Ident("a'"), Ident("b"), Ident("c'"))
            att.state mustEqual List(Ident("a"), Ident("c"))
        }
      }
      "map" in {
        val ast: Ast = Map(Ident("a"), Ident("b"), Ident("c"))
        Subject(Nil, Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) match {
          case (at, att) =>
            at mustEqual Map(Ident("a'"), Ident("b"), Ident("c'"))
            att.state mustEqual List(Ident("a"), Ident("c"))
        }
      }
      "flatMap" in {
        val ast: Ast = FlatMap(Ident("a"), Ident("b"), Ident("c"))
        Subject(Nil, Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) match {
          case (at, att) =>
            at mustEqual FlatMap(Ident("a'"), Ident("b"), Ident("c'"))
            att.state mustEqual List(Ident("a"), Ident("c"))
        }
      }
      "sortBy" in {
        val ast: Ast = SortBy(Ident("a"), Ident("b"), Ident("c"), AscNullsFirst)
        Subject(Nil, Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) match {
          case (at, att) =>
            at mustEqual SortBy(Ident("a'"), Ident("b"), Ident("c'"), AscNullsFirst)
            att.state mustEqual List(Ident("a"), Ident("c"))
        }
      }
      "groupBy" in {
        val ast: Ast = GroupBy(Ident("a"), Ident("b"), Ident("c"))
        Subject(Nil, Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) match {
          case (at, att) =>
            at mustEqual GroupBy(Ident("a'"), Ident("b"), Ident("c'"))
            att.state mustEqual List(Ident("a"), Ident("c"))
        }
      }
      "aggregation" in {
        val ast: Ast = Aggregation(AggregationOperator.max, Ident("a"))
        Subject(Nil, Ident("a") -> Ident("a'"))(ast) match {
          case (at, att) =>
            at mustEqual Aggregation(AggregationOperator.max, Ident("a'"))
            att.state mustEqual List(Ident("a"))
        }
      }
      "take" in {
        val ast: Ast = Take(Ident("a"), Ident("b"))
        Subject(Nil, Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"))(ast) match {
          case (at, att) =>
            at mustEqual Take(Ident("a'"), Ident("b'"))
            att.state mustEqual List(Ident("a"), Ident("b"))
        }
      }
      "drop" in {
        val ast: Ast = Drop(Ident("a"), Ident("b"))
        Subject(Nil, Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"))(ast) match {
          case (at, att) =>
            at mustEqual Drop(Ident("a'"), Ident("b'"))
            att.state mustEqual List(Ident("a"), Ident("b"))
        }
      }
      "union" in {
        val ast: Ast = Union(Ident("a"), Ident("b"))
        Subject(Nil, Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"))(ast) match {
          case (at, att) =>
            at mustEqual Union(Ident("a'"), Ident("b'"))
            att.state mustEqual List(Ident("a"), Ident("b"))
        }
      }
      "unionAll" in {
        val ast: Ast = UnionAll(Ident("a"), Ident("b"))
        Subject(Nil, Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"))(ast) match {
          case (at, att) =>
            at mustEqual UnionAll(Ident("a'"), Ident("b'"))
            att.state mustEqual List(Ident("a"), Ident("b"))
        }
      }
      "outer join" in {
        val ast: Ast = Join(FullJoin, Ident("a"), Ident("b"), Ident("c"), Ident("d"), Ident("e"))
        Subject(Nil, Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("e") -> Ident("e'"))(ast) match {
          case (at, att) =>
            at mustEqual Join(FullJoin, Ident("a'"), Ident("b'"), Ident("c"), Ident("d"), Ident("e'"))
            att.state mustEqual List(Ident("a"), Ident("b"), Ident("e"))
        }
      }
      "distinct" in {
        val ast: Ast = Distinct(Ident("a"))
        Subject(Nil, Ident("a") -> Ident("a'"))(ast) match {
          case (at, att) =>
            at mustEqual Distinct(Ident("a'"))
            att.state mustEqual List(Ident("a"))
        }
      }
    }

    "operation" - {
      "unary" in {
        val ast: Ast = UnaryOperation(BooleanOperator.`!`, Ident("a"))
        Subject(Nil, Ident("a") -> Ident("a'"))(ast) match {
          case (at, att) =>
            at mustEqual UnaryOperation(BooleanOperator.`!`, Ident("a'"))
            att.state mustEqual List(Ident("a"))
        }
      }
      "binary" in {
        val ast: Ast = BinaryOperation(Ident("a"), BooleanOperator.`&&`, Ident("b"))
        Subject(Nil, Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"))(ast) match {
          case (at, att) =>
            at mustEqual BinaryOperation(Ident("a'"), BooleanOperator.`&&`, Ident("b'"))
            att.state mustEqual List(Ident("a"), Ident("b"))
        }
      }
      "function apply" in {
        val ast: Ast = FunctionApply(Ident("a"), List(Ident("b"), Ident("c")))
        Subject(Nil, Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) match {
          case (at, att) =>
            at mustEqual FunctionApply(Ident("a'"), List(Ident("b'"), Ident("c'")))
            att.state mustEqual List(Ident("a"), Ident("b"), Ident("c"))
        }
      }
    }

    "value" - {
      "constant" in {
        val ast: Ast = Constant("a")
        Subject(Nil)(ast) match {
          case (at, att) =>
            at mustEqual ast
            att.state mustEqual Nil
        }
      }
      "null" in {
        val ast: Ast = NullValue
        Subject(Nil)(ast) match {
          case (at, att) =>
            at mustEqual ast
            att.state mustEqual Nil
        }
      }
      "tuple" in {
        val ast: Ast = Tuple(List(Ident("a"), Ident("b"), Ident("c")))
        Subject(Nil, Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) match {
          case (at, att) =>
            at mustEqual Tuple(List(Ident("a'"), Ident("b'"), Ident("c'")))
            att.state mustEqual List(Ident("a"), Ident("b"), Ident("c"))
        }
      }
      "set" in {
        val ast: Ast = Set(List(Ident("a"), Ident("b")))
        Subject(Nil, Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"))(ast) match {
          case (at, att) =>
            at mustEqual Set(List(Ident("a'"), Ident("b'")))
            att.state mustEqual List(Ident("a"), Ident("b"))
        }
      }
    }

    "action" - {
      "update" - {
        "assigned" in {
          val ast: Ast = AssignedAction(Update(Ident("a")), List(Assignment(Ident("b"), "c", Ident("d"))))
          Subject(Nil, Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"), Ident("d") -> Ident("d'"))(ast) match {
            case (at, att) =>
              at mustEqual AssignedAction(Update(Ident("a'")), List(Assignment(Ident("b"), "c", Ident("d'"))))
              att.state mustEqual List(Ident("a"), Ident("d"))
          }
        }
        "unassigned" in {
          val ast: Ast = Update(Ident("a"))
          Subject(Nil, Ident("a") -> Ident("a'"))(ast) match {
            case (at, att) =>
              at mustEqual Update(Ident("a'"))
              att.state mustEqual List(Ident("a"))
          }
        }
      }
      "insert" - {
        "assigned" in {
          val ast: Ast = AssignedAction(Insert(Ident("a")), List(Assignment(Ident("b"), "c", Ident("d"))))
          Subject(Nil, Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"), Ident("d") -> Ident("d'"))(ast) match {
            case (at, att) =>
              at mustEqual AssignedAction(Insert(Ident("a'")), List(Assignment(Ident("b"), "c", Ident("d'"))))
              att.state mustEqual List(Ident("a"), Ident("d"))
          }
        }
        "unassigned" in {
          val ast: Ast = Insert(Ident("a"))
          Subject(Nil, Ident("a") -> Ident("a'"))(ast) match {
            case (at, att) =>
              at mustEqual Insert(Ident("a'"))
              att.state mustEqual List(Ident("a"))
          }
        }
      }
      "delete" in {
        val ast: Ast = Delete(Ident("a"))
        Subject(Nil, Ident("a") -> Ident("a'"))(ast) match {
          case (at, att) =>
            at mustEqual Delete(Ident("a'"))
            att.state mustEqual List(Ident("a"))
        }
      }
    }

    "function" in {
      val ast: Ast = Function(List(Ident("a")), Ident("a"))
      Subject(Nil, Ident("a") -> Ident("a'"))(ast) match {
        case (at, att) =>
          at mustEqual Function(List(Ident("a")), Ident("a'"))
          att.state mustEqual List(Ident("a"))
      }
    }

    "ident" in {
      val ast: Ast = Ident("a")
      Subject(Nil, Ident("a") -> Ident("a'"))(ast) match {
        case (at, att) =>
          at mustEqual Ident("a'")
          att.state mustEqual List(Ident("a"))
      }
    }

    "property" in {
      val ast: Ast = Property(Ident("a"), "b")
      Subject(Nil, Ident("a") -> Ident("a'"))(ast) match {
        case (at, att) =>
          at mustEqual Property(Ident("a'"), "b")
          att.state mustEqual List(Ident("a"))
      }
    }

    "infix" in {
      val ast: Ast = Infix(List("test"), List(Ident("a")))
      Subject(Nil, Ident("a") -> Ident("a'"))(ast) match {
        case (at, att) =>
          at mustEqual Infix(List("test"), List(Ident("a'")))
          att.state mustEqual List(Ident("a"))
      }
    }

    "option operation" in {
      val ast: Ast = OptionOperation(OptionMap, Ident("a"), Ident("b"), Ident("c"))
      Subject(Nil, Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) match {
        case (at, att) =>
          at mustEqual OptionOperation(OptionMap, Ident("a'"), Ident("b"), Ident("c'"))
          att.state mustEqual List(Ident("a"), Ident("c"))
      }
    }

    "if" in {
      val ast: Ast = If(Ident("a"), Ident("b"), Ident("c"))
      Subject(Nil, Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) match {
        case (at, att) =>
          at mustEqual If(Ident("a'"), Ident("b'"), Ident("c'"))
          att.state mustEqual List(Ident("a"), Ident("b"), Ident("c"))
      }
    }

    "dynamic" in {
      val ast: Ast = Dynamic(Ident("a"))
      Subject(Nil, Ident("a") -> Ident("a'"))(ast) match {
        case (at, att) =>
          at mustEqual ast
          att.state mustEqual List()
      }
    }

    "block" in {
      val ast: Ast = Block(List(
        Val(Ident("a"), Entity("a")),
        Val(Ident("b"), Entity("b"))
      ))
      Subject(Nil, Entity("a") -> Entity("b"), Entity("b") -> Entity("c"))(ast) match {
        case (at, att) =>
          at mustEqual Block(List(
            Val(Ident("a"), Entity("b")),
            Val(Ident("b"), Entity("c"))
          ))
          att.state mustEqual List(Entity("a"), Entity("b"))
      }
    }

    "val" in {
      val ast: Ast = Val(Ident("a"), Entity("a"))
      Subject(Nil, Entity("a") -> Entity("b"))(ast) match {
        case (at, att) =>
          at mustEqual Val(Ident("a"), Entity("b"))
          att.state mustEqual List(Entity("a"))
      }
    }
  }
}
