package io.getquill.norm

import io.getquill._
import io.getquill.ast._

class BetaReductionSpec extends Spec {

  "simplifies the ast by applying functons" - {
    "tuple field" in {
      val ast: Ast = Property(Tuple(List(Ident("a"))), "_1")
      BetaReduction(ast) mustEqual Ident("a")
    }
    "function apply" in {
      val function = Function(List(Ident("a")), Ident("a"))
      val ast: Ast = FunctionApply(function, List(Ident("b")))
      BetaReduction(ast) mustEqual Ident("b")
    }
  }

  "replaces identifiers by actuals" - {
    "ident" in {
      val ast: Ast = Ident("a")
      BetaReduction(ast, Ident("a") -> Ident("a'")) mustEqual
        Ident("a'")
    }
    "avoids replacing idents of an outer scope" - {
      "function" in {
        val ast: Ast = Function(List(Ident("a")), Ident("a"))
        BetaReduction(ast, Ident("a") -> Ident("a'")) mustEqual ast
      }
      "filter" in {
        val ast: Ast = Filter(Ident("a"), Ident("b"), Ident("b"))
        BetaReduction(ast, Ident("b") -> Ident("b'")) mustEqual ast
      }
      "map" in {
        val ast: Ast = Map(Ident("a"), Ident("b"), Ident("b"))
        BetaReduction(ast, Ident("b") -> Ident("b'")) mustEqual ast
      }
      "flatMap" in {
        val ast: Ast = FlatMap(Ident("a"), Ident("b"), Ident("b"))
        BetaReduction(ast, Ident("b") -> Ident("b'")) mustEqual ast
      }
      "sortBy" in {
        val ast: Ast = SortBy(Ident("a"), Ident("b"), Ident("b"))
        BetaReduction(ast, Ident("b") -> Ident("b'")) mustEqual ast
      }
      "groupBy" in {
        val ast: Ast = GroupBy(Ident("a"), Ident("b"), Ident("b"))
        BetaReduction(ast, Ident("b") -> Ident("b'")) mustEqual ast
      }
      "reverse" in {
        val ast: Ast = Reverse(SortBy(Ident("a"), Ident("b"), Ident("b")))
        BetaReduction(ast, Ident("b") -> Ident("b'")) mustEqual ast
      }
      "outer join" in {
        val ast: Ast = OuterJoin(LeftJoin, Ident("a"), Ident("b"), Ident("c"), Ident("d"), Tuple(List(Ident("c"), Ident("d"))))
        BetaReduction(ast, Ident("c") -> Ident("c'"), Ident("d") -> Ident("d'")) mustEqual ast
      }
      "option operation" in {
        val ast: Ast = OptionOperation(OptionMap, Ident("a"), Ident("b"), Ident("b"))
        BetaReduction(ast, Ident("b") -> Ident("b'")) mustEqual ast
      }
    }
  }

  "reapplies the beta reduction if the structure changes" in {
    val ast: Ast = Property(Ident("a"), "_1")
    BetaReduction(ast, Ident("a") -> Tuple(List(Ident("a'")))) mustEqual
      Ident("a'")
  }
}
