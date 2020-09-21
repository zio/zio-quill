package io.getquill.ast

import io.getquill.Spec
import io.getquill.ast.Renameable.Fixed
import io.getquill.ast.Visibility.Visible

class StatelessTransformerSpec extends Spec {

  case class Subject(replace: (Ast, Ast)*) extends StatelessTransformer {
    override def apply(e: Ast): Ast =
      replace.toMap.getOrElse(e, super.apply(e))
  }

  "transforms asts" - {
    "query" - {
      "entity" in {
        val ast: Ast = Entity("a", Nil, QEP)
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
      "concatMap" in {
        val ast: Ast = ConcatMap(Ident("a"), Ident("b"), Ident("c"))
        Subject(Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) mustEqual
          ConcatMap(Ident("a'"), Ident("b"), Ident("c'"))
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
      "flat join" in {
        val ast: Ast = FlatJoin(InnerJoin, Ident("a"), Ident("b"), Ident("c"))
        Subject(Ident("a") -> Ident("a'"), Ident("c") -> Ident("c'"))(ast) mustEqual
          FlatJoin(InnerJoin, Ident("a'"), Ident("b"), Ident("c'"))
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
        val ast: Ast = Constant.auto("a")
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
      "caseclass" in {
        val ast: Ast = CaseClass(List(("foo", Ident("a")), ("bar", Ident("b")), ("baz", Ident("c"))))
        Subject(Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) mustEqual
          CaseClass(List(("foo", Ident("a'")), ("bar", Ident("b'")), ("baz", Ident("c'"))))
      }
    }

    "Assignment" in {
      val ast: Ast = Assignment(Ident("a"), Ident("b"), Ident("b"))
      Subject(Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) mustEqual
        Assignment(Ident("a"), Ident("b'"), Ident("b'"))
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
      "onConflict" in {
        val ast: Ast = OnConflict(Insert(Ident("a"), Nil), OnConflict.NoTarget, OnConflict.Ignore)
        Subject(Ident("a") -> Ident("a'"))(ast) mustEqual
          OnConflict(Insert(Ident("a'"), Nil), OnConflict.NoTarget, OnConflict.Ignore)
      }
    }

    "onConflict.target" - {
      "no" in {
        val target: OnConflict.Target = OnConflict.NoTarget
        Subject()(target) mustEqual target
      }
      "properties" in {
        val target: OnConflict.Target = OnConflict.Properties(List(Property(Ident("a"), "b")))
        Subject(Ident("a") -> Ident("a'"))(target) mustEqual
          OnConflict.Properties(List(Property(Ident("a'"), "b")))
      }
      "properties - fixed" in {
        val target: OnConflict.Target = OnConflict.Properties(List(Property.Opinionated(Ident("a"), "b", Fixed, Visible)))
        Subject(Ident("a") -> Ident("a'"))(target) mustEqual
          OnConflict.Properties(List(Property.Opinionated(Ident("a'"), "b", Fixed, Visible)))
      }
    }

    "onConflict.action" - {
      "ignore" in {
        val action: OnConflict.Action = OnConflict.Ignore
        Subject()(action) mustEqual action
      }
      "update" in {
        val action: OnConflict.Action = OnConflict.Update(List(Assignment(Ident("a"), Ident("b"), Ident("c"))))
        Subject(Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(action) mustEqual
          OnConflict.Update(List(Assignment(Ident("a"), Ident("b'"), Ident("c'"))))
      }
    }

    "onConflict.excluded" in {
      val ast: Ast = OnConflict.Excluded(Ident("a"))
      Subject()(ast) mustEqual ast
    }

    "onConflict.existing" in {
      val ast: Ast = OnConflict.Existing(Ident("a"))
      Subject()(ast) mustEqual ast
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

    "property - fixed" in {
      val ast: Ast = Property.Opinionated(Ident("a"), "b", Fixed, Visible)
      Subject(Ident("a") -> Ident("a'"))(ast) mustEqual
        Property.Opinionated(Ident("a'"), "b", Fixed, Visible)
    }

    "infix" in {
      val ast: Ast = Infix(List("test"), List(Ident("a"), Ident("b")), false, QV)
      Subject(Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"))(ast) mustEqual
        Infix(List("test"), List(Ident("a'"), Ident("b'")), false, QV)
    }

    "infix - pure" in {
      val ast: Ast = Infix(List("test"), List(Ident("a"), Ident("b")), true, QV)
      Subject(Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"))(ast) mustEqual
        Infix(List("test"), List(Ident("a'"), Ident("b'")), true, QV)
    }

    "option operation" - {
      "flatten" in {
        val ast: Ast = OptionFlatten(Ident("a"))
        Subject(Ident("a") -> Ident("a'"))(ast) mustEqual
          OptionFlatten(Ident("a'"))
      }
      "Some" in {
        val ast: Ast = OptionSome(Ident("a"))
        Subject(Ident("a") -> Ident("a'"))(ast) mustEqual
          OptionSome(Ident("a'"))
      }
      "apply" in {
        val ast: Ast = OptionApply(Ident("a"))
        Subject(Ident("a") -> Ident("a'"))(ast) mustEqual
          OptionApply(Ident("a'"))
      }
      "orNull" in {
        val ast: Ast = OptionOrNull(Ident("a"))
        Subject(Ident("a") -> Ident("a'"))(ast) mustEqual
          OptionOrNull(Ident("a'"))
      }
      "getOrNull" in {
        val ast: Ast = OptionGetOrNull(Ident("a"))
        Subject(Ident("a") -> Ident("a'"))(ast) mustEqual
          OptionGetOrNull(Ident("a'"))
      }
      "None" in {
        val ast: Ast = OptionNone(QV)
        Subject()(ast) mustEqual ast
      }
      "getOrElse" in {
        val ast: Ast = OptionGetOrElse(Ident("a"), Ident("b"))
        Subject(Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"))(ast) mustEqual
          OptionGetOrElse(Ident("a'"), Ident("b'"))
      }
      "flatMap - Unchecked" in {
        val ast: Ast = OptionTableFlatMap(Ident("a"), Ident("b"), Ident("c"))
        Subject(Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) mustEqual
          OptionTableFlatMap(Ident("a'"), Ident("b"), Ident("c'"))
      }
      "map - Unchecked" in {
        val ast: Ast = OptionTableMap(Ident("a"), Ident("b"), Ident("c"))
        Subject(Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) mustEqual
          OptionTableMap(Ident("a'"), Ident("b"), Ident("c'"))
      }
      "flatMap" in {
        val ast: Ast = OptionFlatMap(Ident("a"), Ident("b"), Ident("c"))
        Subject(Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) mustEqual
          OptionFlatMap(Ident("a'"), Ident("b"), Ident("c'"))
      }
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
      "forall - Unchecked" in {
        val ast: Ast = OptionTableForall(Ident("a"), Ident("b"), Ident("c"))
        Subject(Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) mustEqual
          OptionTableForall(Ident("a'"), Ident("b"), Ident("c'"))
      }
      "exists" in {
        val ast: Ast = OptionExists(Ident("a"), Ident("b"), Ident("c"))
        Subject(Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) mustEqual
          OptionExists(Ident("a'"), Ident("b"), Ident("c'"))
      }
      "exists - Unchecked" in {
        val ast: Ast = OptionTableExists(Ident("a"), Ident("b"), Ident("c"))
        Subject(Ident("a") -> Ident("a'"), Ident("b") -> Ident("b'"), Ident("c") -> Ident("c'"))(ast) mustEqual
          OptionTableExists(Ident("a'"), Ident("b"), Ident("c'"))
      }
      "contains" in {
        val ast: Ast = OptionContains(Ident("a"), Ident("c"))
        Subject(Ident("a") -> Ident("a'"), Ident("c") -> Ident("c'"))(ast) mustEqual
          OptionContains(Ident("a'"), Ident("c'"))
      }
      "isEmpty" in {
        val ast: Ast = OptionIsEmpty(Ident("a"))
        Subject(Ident("a") -> Ident("a'"))(ast) mustEqual
          OptionIsEmpty(Ident("a'"))
      }
      "nonEmpty" in {
        val ast: Ast = OptionNonEmpty(Ident("a"))
        Subject(Ident("a") -> Ident("a'"))(ast) mustEqual
          OptionNonEmpty(Ident("a'"))
      }
      "isDefined" in {
        val ast: Ast = OptionIsDefined(Ident("a"))
        Subject(Ident("a") -> Ident("a'"))(ast) mustEqual
          OptionIsDefined(Ident("a'"))
      }
    }

    "traversable operations" - {
      "map.contains" in {
        val ast: Ast = MapContains(Ident("a"), Ident("c"))
        Subject(Ident("a") -> Ident("a'"), Ident("c") -> Ident("c'"))(ast) mustEqual
          MapContains(Ident("a'"), Ident("c'"))
      }
      "set.contains" in {
        val ast: Ast = SetContains(Ident("a"), Ident("c"))
        Subject(Ident("a") -> Ident("a'"), Ident("c") -> Ident("c'"))(ast) mustEqual
          SetContains(Ident("a'"), Ident("c'"))
      }
      "list.contains" in {
        val ast: Ast = ListContains(Ident("a"), Ident("c"))
        Subject(Ident("a") -> Ident("a'"), Ident("c") -> Ident("c'"))(ast) mustEqual
          ListContains(Ident("a'"), Ident("c'"))
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
        Val(Ident("a"), Entity("a", Nil, QEP)),
        Val(Ident("b"), Entity("b", Nil, QEP))
      ))
      Subject(Entity("a", Nil, QEP) -> Entity("b", Nil, QEP), Entity("b", Nil, QEP) -> Entity("c", Nil, QEP))(ast) mustEqual
        Block(List(
          Val(Ident("a"), Entity("b", Nil, QEP)),
          Val(Ident("b"), Entity("c", Nil, QEP))
        ))
    }

    "val" in {
      val ast: Ast = Val(Ident("a"), Entity("a", Nil, QEP))
      Subject(Entity("a", Nil, QEP) -> Entity("b", Nil, QEP))(ast) mustEqual
        Val(Ident("a"), Entity("b", Nil, QEP))
    }
  }
}
