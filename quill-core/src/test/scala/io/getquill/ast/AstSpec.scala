package io.getquill.ast

import io.getquill.Spec
import io.getquill.testSource._

class AstSpec extends Spec {

  "overrides toString to ease debugging" in {
    val q =
      quote {
        query[TestEntity].filter(t => t.s == "test")
      }
    q.ast.toString mustEqual """query[TestEntity].filter(t => t.s == "test")"""
  }
}
