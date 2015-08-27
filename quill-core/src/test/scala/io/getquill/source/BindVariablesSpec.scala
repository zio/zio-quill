package io.getquill.source

import io.getquill.Spec
import io.getquill._
import io.getquill.ast._

class BindVariablesSpec extends Spec {

  "replaces the binded values by '?' and returns the positional list of bindings" in {
    val q = quote {
      (a: Int, b: Int) => qr1.filter(t => t.i == (b + a))
    }
    val (ast, bindings) = BindVariables(q.ast.body, List(Ident("a"), Ident("b")))
    ast.toString mustEqual "queryable[TestEntity].filter(t => t.i == (? + ?))"
    bindings mustEqual List(Ident("b"), Ident("a"))
  }
}
