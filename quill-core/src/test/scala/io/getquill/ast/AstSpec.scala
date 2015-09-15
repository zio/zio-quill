package io.getquill.ast

import scala.language.reflectiveCalls

import io.getquill._
import io.getquill.queryable
import io.getquill.quote

class AstSpec extends Spec {

  "overrides toString to ease debugging" in {
    val q =
      quote {
        queryable[TestEntity].filter(t => t.s == "test")
      }
    q.ast.toString mustEqual """queryable[TestEntity].filter(t => t.s == "test")"""
  }
}
