package io.getquill.ast

import io.getquill._
import io.getquill.query
import io.getquill.quote

class AstSpec extends Spec {

  "overrides toString to ease debugging" in {
    val q =
      quote {
        query[TestEntity].filter(t => t.s == "test")
      }
    q.ast.toString mustEqual """query[TestEntity].filter(t => t.s == "test")"""
  }
}
