package io.getquill.ast

import io.getquill._
import test.Spec
import language.reflectiveCalls

class AstSpec extends Spec {

  "overrides toString to ease debugging" in {
    val q =
      quote {
        queryable[TestEntity].filter(t => t.s == "test")
      }
    q.ast.toString mustEqual """queryable[TestEntity].filter(t => t.s == "test")"""
  }
}
