package io.getquill.ast

import io.getquill.Spec
import io.getquill.ast.Renameable.Fixed
import io.getquill.ast.Visibility.Visible

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
        val ast: Ast = Entity("a", Nil, QEP)
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
      "concatMap" in {
        val ast: Ast = ConcatMap(Ident("a"), Ident("b"), Ident("c"))
        Subject(Nil, Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) match {
          case (at, att) =>
            at mustEqual ConcatMap(Ident("a'"), Ident("b"), Ident("c'"))
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
        val ast: Ast = Constant.auto("a")
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
      "caseclass" in {
        val ast: Ast = CaseClass(List(("foo", Ident("a")), ("bar", Ident("b")), ("baz", Ident("c"))))
        Subject(Nil, Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) match {
          case (at, att) =>
            at mustEqual CaseClass(List(("foo", Ident("a'")), ("bar", Ident("b'")), ("baz", Ident("c'"))))
            att.state mustEqual List(Ident("a"), Ident("b"), Ident("c"))
        }
      }
    }

    "action" - {
      "update" in {
        val ast: Ast = Update(Ident("a"), List(Assignment(Ident("b"), Ident("c"), Ident("d"))))
        Subject(Nil, Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"), Ident("d") -> Ident("d'"))(ast) match {
          case (at, att) =>
            at mustEqual Update(Ident("a'"), List(Assignment(Ident("b"), Ident("c'"), Ident("d'"))))
            att.state mustEqual List(Ident("a"), Ident("c"), Ident("d"))
        }
      }
      "insert" in {
        val ast: Ast = Insert(Ident("a"), List(Assignment(Ident("b"), Ident("c"), Ident("d"))))
        Subject(Nil, Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"), Ident("d") -> Ident("d'"))(ast) match {
          case (at, att) =>
            at mustEqual Insert(Ident("a'"), List(Assignment(Ident("b"), Ident("c'"), Ident("d'"))))
            att.state mustEqual List(Ident("a"), Ident("c"), Ident("d"))
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
      "onConflict" in {
        val ast: Ast = OnConflict(Insert(Ident("a"), Nil), OnConflict.NoTarget, OnConflict.Ignore)
        Subject(Nil, Ident("a") -> Ident("a'"))(ast) match {
          case (at, att) =>
            at mustEqual OnConflict(Insert(Ident("a'"), Nil), OnConflict.NoTarget, OnConflict.Ignore)
            att.state mustEqual List(Ident("a"))
        }
      }
    }

    "onConflict.target" - {
      "no" in {
        val target: OnConflict.Target = OnConflict.NoTarget
        Subject(Nil)(target) match {
          case (at, att) =>
            at mustEqual target
            att.state mustEqual Nil
        }
      }
      "properties" in {
        val target: OnConflict.Target = OnConflict.Properties(List(Property(Ident("a"), "b")))
        Subject(Nil, Ident("a") -> Ident("a'"))(target) match {
          case (at, att) =>
            at mustEqual OnConflict.Properties(List(Property(Ident("a'"), "b")))
            att.state mustEqual List(Ident("a"))
        }
      }
    }

    "onConflict.action" - {
      "ignore" in {
        val action: OnConflict.Action = OnConflict.Ignore
        Subject(Nil)(action) match {
          case (at, att) =>
            at mustEqual action
            att.state mustEqual Nil
        }
      }
      "update" in {
        val action: OnConflict.Action = OnConflict.Update(List(Assignment(Ident("a"), Ident("b"), Ident("c"))))
        Subject(Nil, Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(action) match {
          case (at, att) =>
            at mustEqual OnConflict.Update(List(Assignment(Ident("a"), Ident("b'"), Ident("c'"))))
            att.state mustEqual List(Ident("b"), Ident("c"))
        }
      }
    }

    "onConflict.excluded" in {
      val ast: Ast = OnConflict.Excluded(Ident("a"))
      Subject(Nil)(ast) match {
        case (at, att) =>
          at mustEqual ast
          att.state mustEqual Nil
      }
    }

    "onConflict.existing" in {
      val ast: Ast = OnConflict.Existing(Ident("a"))
      Subject(Nil)(ast) match {
        case (at, att) =>
          at mustEqual ast
          att.state mustEqual Nil
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

    "property - fixed" in {
      val ast: Ast = Property.Opinionated(Ident("a"), "b", Fixed, Visible)
      Subject(Nil, Ident("a") -> Ident("a'"))(ast) match {
        case (at, att) =>
          at mustEqual Property.Opinionated(Ident("a'"), "b", Fixed, Visible)
          att.state mustEqual List(Ident("a"))
      }
    }

    "quotedReference" in {
      val ast: Ast = QuotedReference(null, Ident("a"))
      Subject(Nil, Ident("a") -> Ident("a'"))(ast) match {
        case (at, att) =>
          at mustEqual QuotedReference(null, Ident("a'"))
          att.state mustEqual List(Ident("a"))
      }
    }

    "infix" in {
      val ast: Ast = Infix(List("test"), List(Ident("a")), false, QV)
      Subject(Nil, Ident("a") -> Ident("a'"))(ast) match {
        case (at, att) =>
          at mustEqual Infix(List("test"), List(Ident("a'")), false, QV)
          att.state mustEqual List(Ident("a"))
      }
    }

    "infix - pure" in {
      val ast: Ast = Infix(List("test"), List(Ident("a")), true, QV)
      Subject(Nil, Ident("a") -> Ident("a'"))(ast) match {
        case (at, att) =>
          at mustEqual Infix(List("test"), List(Ident("a'")), true, QV)
          att.state mustEqual List(Ident("a"))
      }
    }

    "option operation" - {
      "flatten" in {
        val ast: Ast = OptionFlatten(Ident("a"))
        Subject(Nil, Ident("a") -> Ident("a'"))(ast) match {
          case (at, att) =>
            at mustEqual OptionFlatten(Ident("a'"))
            att.state mustEqual List(Ident("a"))
        }
      }
      "Some" in {
        val ast: Ast = OptionSome(Ident("a"))
        Subject(Nil, Ident("a") -> Ident("a'"))(ast) match {
          case (at, att) =>
            at mustEqual OptionSome(Ident("a'"))
            att.state mustEqual List(Ident("a"))
        }
      }
      "apply" in {
        val ast: Ast = OptionApply(Ident("a"))
        Subject(Nil, Ident("a") -> Ident("a'"))(ast) match {
          case (at, att) =>
            at mustEqual OptionApply(Ident("a'"))
            att.state mustEqual List(Ident("a"))
        }
      }
      "orNull" in {
        val ast: Ast = OptionOrNull(Ident("a"))
        Subject(Nil, Ident("a") -> Ident("a'"))(ast) match {
          case (at, att) =>
            at mustEqual OptionOrNull(Ident("a'"))
            att.state mustEqual List(Ident("a"))
        }
      }
      "getOrNull" in {
        val ast: Ast = OptionGetOrNull(Ident("a"))
        Subject(Nil, Ident("a") -> Ident("a'"))(ast) match {
          case (at, att) =>
            at mustEqual OptionGetOrNull(Ident("a'"))
            att.state mustEqual List(Ident("a"))
        }
      }
      "None" in {
        val ast: Ast = OptionNone(QV)
        Subject(Nil)(ast) match {
          case (at, att) =>
            at mustEqual ast
            att.state mustEqual Nil
        }
      }
      "getOrElse" in {
        val ast: Ast = OptionGetOrElse(Ident("a"), Ident("b"))
        Subject(Nil, Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"))(ast) match {
          case (at, att) =>
            at mustEqual OptionGetOrElse(Ident("a'"), Ident("b'"))
            att.state mustEqual List(Ident("a"), Ident("b"))
        }
      }
      "flatMap - Unchecked" in {
        val ast: Ast = OptionTableFlatMap(Ident("a"), Ident("b"), Ident("c"))
        Subject(Nil, Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) match {
          case (at, att) =>
            at mustEqual OptionTableFlatMap(Ident("a'"), Ident("b"), Ident("c'"))
            att.state mustEqual List(Ident("a"), Ident("c"))
        }
      }
      "map - Unchecked" in {
        val ast: Ast = OptionTableMap(Ident("a"), Ident("b"), Ident("c"))
        Subject(Nil, Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) match {
          case (at, att) =>
            at mustEqual OptionTableMap(Ident("a'"), Ident("b"), Ident("c'"))
            att.state mustEqual List(Ident("a"), Ident("c"))
        }
      }
      "flatMap" in {
        val ast: Ast = OptionFlatMap(Ident("a"), Ident("b"), Ident("c"))
        Subject(Nil, Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) match {
          case (at, att) =>
            at mustEqual OptionFlatMap(Ident("a'"), Ident("b"), Ident("c'"))
            att.state mustEqual List(Ident("a"), Ident("c"))
        }
      }
      "map" in {
        val ast: Ast = OptionMap(Ident("a"), Ident("b"), Ident("c"))
        Subject(Nil, Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) match {
          case (at, att) =>
            at mustEqual OptionMap(Ident("a'"), Ident("b"), Ident("c'"))
            att.state mustEqual List(Ident("a"), Ident("c"))
        }
      }
      "forall" in {
        val ast: Ast = OptionForall(Ident("a"), Ident("b"), Ident("c"))
        Subject(Nil, Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) match {
          case (at, att) =>
            at mustEqual OptionForall(Ident("a'"), Ident("b"), Ident("c'"))
            att.state mustEqual List(Ident("a"), Ident("c"))
        }
      }
      "forall - Unchecked" in {
        val ast: Ast = OptionTableForall(Ident("a"), Ident("b"), Ident("c"))
        Subject(Nil, Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) match {
          case (at, att) =>
            at mustEqual OptionTableForall(Ident("a'"), Ident("b"), Ident("c'"))
            att.state mustEqual List(Ident("a"), Ident("c"))
        }
      }
      "exists - Unchecked" in {
        val ast: Ast = OptionTableExists(Ident("a"), Ident("b"), Ident("c"))
        Subject(Nil, Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) match {
          case (at, att) =>
            at mustEqual OptionTableExists(Ident("a'"), Ident("b"), Ident("c'"))
            att.state mustEqual List(Ident("a"), Ident("c"))
        }
      }
      "exists" in {
        val ast: Ast = OptionExists(Ident("a"), Ident("b"), Ident("c"))
        Subject(Nil, Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) match {
          case (at, att) =>
            at mustEqual OptionExists(Ident("a'"), Ident("b"), Ident("c'"))
            att.state mustEqual List(Ident("a"), Ident("c"))
        }
      }
      "contains" in {
        val ast: Ast = OptionContains(Ident("a"), Ident("c"))
        Subject(Nil, Ident("a") -> Ident("a'"), Ident("c") -> Ident("c'"))(ast) match {
          case (at, att) =>
            at mustEqual OptionContains(Ident("a'"), Ident("c'"))
            att.state mustEqual List(Ident("a"), Ident("c"))
        }
      }
      "isEmpty" in {
        val ast: Ast = OptionIsEmpty(Ident("a"))
        Subject(Nil, Ident("a") -> Ident("a'"))(ast) match {
          case (at, att) =>
            at mustEqual OptionIsEmpty(Ident("a'"))
            att.state mustEqual List(Ident("a"))
        }
      }
      "nonEmpty" in {
        val ast: Ast = OptionNonEmpty(Ident("a"))
        Subject(Nil, Ident("a") -> Ident("a'"))(ast) match {
          case (at, att) =>
            at mustEqual OptionNonEmpty(Ident("a'"))
            att.state mustEqual List(Ident("a"))
        }
      }
      "isDefined" in {
        val ast: Ast = OptionIsDefined(Ident("a"))
        Subject(Nil, Ident("a") -> Ident("a'"))(ast) match {
          case (at, att) =>
            at mustEqual OptionIsDefined(Ident("a'"))
            att.state mustEqual List(Ident("a"))
        }
      }
    }

    "traversable operations" - {
      "map.contains" in {
        val ast: Ast = MapContains(Ident("a"), Ident("c"))
        Subject(Nil, Ident("a") -> Ident("a'"), Ident("c") -> Ident("c'"))(ast) match {
          case (at, att) =>
            at mustEqual MapContains(Ident("a'"), Ident("c'"))
            att.state mustEqual List(Ident("a"), Ident("c"))
        }
      }
      "set.contains" in {
        val ast: Ast = SetContains(Ident("a"), Ident("c"))
        Subject(Nil, Ident("a") -> Ident("a'"), Ident("c") -> Ident("c'"))(ast) match {
          case (at, att) =>
            at mustEqual SetContains(Ident("a'"), Ident("c'"))
            att.state mustEqual List(Ident("a"), Ident("c"))
        }
      }
      "list.contains" in {
        val ast: Ast = ListContains(Ident("a"), Ident("c"))
        Subject(Nil, Ident("a") -> Ident("a'"), Ident("c") -> Ident("c'"))(ast) match {
          case (at, att) =>
            at mustEqual ListContains(Ident("a'"), Ident("c'"))
            att.state mustEqual List(Ident("a"), Ident("c"))
        }
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
        Val(Ident("a"), Entity("a", Nil, QEP)),
        Val(Ident("b"), Entity("b", Nil, QEP))
      ))
      Subject(Nil, Entity("a", Nil, QEP) -> Entity("b", Nil, QEP), Entity("b", Nil, QEP) -> Entity("c", Nil, QEP))(ast) match {
        case (at, att) =>
          at mustEqual Block(List(
            Val(Ident("a"), Entity("b", Nil, QEP)),
            Val(Ident("b"), Entity("c", Nil, QEP))
          ))
          att.state mustEqual List(Entity("a", Nil, QEP), Entity("b", Nil, QEP))
      }
    }

    "val" in {
      val ast: Ast = Val(Ident("a"), Entity("a", Nil, QEP))
      Subject(Nil, Entity("a", Nil, QEP) -> Entity("b", Nil, QEP))(ast) match {
        case (at, att) =>
          at mustEqual Val(Ident("a"), Entity("b", Nil, QEP))
          att.state mustEqual List(Entity("a", Nil, QEP))
      }
    }
  }
}
