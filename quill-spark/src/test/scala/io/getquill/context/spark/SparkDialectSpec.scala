package io.getquill.context.spark

import io.getquill.Spec
import io.getquill.Literal

class SparkDialectSpec extends Spec {

  import testContext._

  "liftingPlaceholder" in {
    SparkDialect.liftingPlaceholder(1) mustEqual "?"
  }

  "prepareForProbing" in {
    val string = "some string"
    SparkDialect.prepareForProbing(string) mustEqual string
  }

  "translate" - {
    "query" in {
      val ast = query[Test].ast
      val (norm, stmt) = SparkDialect.translate(ast)(Literal)
      norm mustEqual ast
      stmt.toString mustEqual "SELECT x.* FROM Test x"
    }
    "non-query" in {
      val ast = infix"SELECT 1".ast
      val (norm, stmt) = SparkDialect.translate(ast)(Literal)
      norm mustEqual ast
      stmt.toString mustEqual "SELECT 1"
    }
  }

  "nested property" in {
    case class Inner(i: Int)
    case class Outer(inner: Inner)
    val ast = query[Outer].filter(t => t.inner.i == 1).ast
    val (norm, stmt) = SparkDialect.translate(ast)(Literal)
    norm mustEqual ast
    stmt.toString mustEqual "SELECT t.* FROM Outer t WHERE t.inner.i = 1"
  }

  "nested tuple" in {
    val ast = query[Test].map(t => ((t.i, t.j), t.i + 1)).ast
    val (norm, stmt) = SparkDialect.translate(ast)(Literal)
    norm mustEqual ast
    stmt.toString mustEqual "SELECT (t.i, t.j) _1, t.i + 1 _2 FROM Test t"
  }
}