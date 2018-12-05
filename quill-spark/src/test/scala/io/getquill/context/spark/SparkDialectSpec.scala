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

  "escapes ' " in {
    val ast = query[Test].map(t => "test'").ast
    val (norm, stmt) = SparkDialect.translate(ast)(Literal)
    norm mustEqual ast
    stmt.toString mustEqual "SELECT 'test\\'' AS _1 FROM Test t"
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
    stmt.toString mustEqual "SELECT (t.i, t.j) AS _1, t.i + 1 AS _2 FROM Test t"
  }

  "concatMap" in {
    val ast = query[Test].concatMap(t => t.s.split(" ")).ast
    val (norm, stmt) = SparkDialect.translate(ast)(Literal)
    norm mustEqual ast
    stmt.toString mustEqual "SELECT explode(SPLIT(t.s, ' ')) AS _1 FROM Test t"
  }

  "non-tuple select" in {
    val ast = query[Test].concatMap(t => t.s.split(" ")).filter(s => s == "s").ast
    val (norm, stmt) = SparkDialect.translate(ast)(Literal)
    norm mustEqual ast
    stmt.toString mustEqual "SELECT s.* FROM (SELECT explode(SPLIT(t.s, ' ')) AS _1 FROM Test t) AS s WHERE s._1 = 's'"
  }

  "concat string" in {
    val ast = query[Test].map(t => t.s + " ").ast
    val (norm, stmt) = SparkDialect.translate(ast)(Literal)
    norm mustEqual ast
    stmt.toString mustEqual "SELECT concat(t.s, ' ') AS _1 FROM Test t"
  }

  "groupBy with multiple columns" in {
    val ast = query[Test].groupBy(t => (t.i, t.j)).map(t => t._2).ast
    val (norm, stmt) = SparkDialect.translate(ast)(Literal)
    norm mustEqual ast
    stmt.toString mustEqual "SELECT t.* FROM Test t GROUP BY t.i, t.j"
  }
}