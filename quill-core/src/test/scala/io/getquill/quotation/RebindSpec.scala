package io.getquill.quotation

import io.getquill.Spec
import io.getquill.testContext.Action
import io.getquill.testContext.InfixInterpolator
import io.getquill.testContext.TestEntity
import io.getquill.testContext.query
import io.getquill.testContext.quote
import io.getquill.testContext.unquote
import io.getquill.testContext

class RebindSpec extends Spec {

  "rebind non-arg function" - {
    "with type param" in {
      implicit class ReturnId[T, O](action: Action[T, O]) {
        def returnId[ID] = quote(infix"$action RETURNING ID".as[Action[T, ID]])
      }
      val q = quote { (i: Int) =>
        unquote(query[TestEntity].insert(e => e.i -> i).returnId[Long])
      }
      testContext.run(q)(List(1)).ast.toString must equal("infix\"" + "$" + "{query[TestEntity].insert(e => e.i -> p1)} RETURNING ID\"")
    }

    "with no type param" in {
      implicit class ReturnId[T, O](action: Action[T, O]) {
        def returnId = quote(infix"$action RETURNING ID".as[Action[T, Long]])
      }
      val q = quote { (i: Int) =>
        unquote(query[TestEntity].insert(e => e.i -> i).returnId)
      }
      testContext.run(q)(List(1)).ast.toString must equal("infix\"" + "$" + "{query[TestEntity].insert(e => e.i -> p1)} RETURNING ID\"")
    }
  }

  "rebind function with args and" - {
    "type param" in {
      implicit class Inc(field: Int) {
        def plus[T](delta: T) = quote(infix"$field + $delta".as[T])
      }

      val q = quote(query[TestEntity].map(e => unquote(e.i.plus(10))))
      testContext.run(q).ast.toString must equal("query[TestEntity].map(e => infix\"" + "$" + "{e.i} + " + "$" + "{10}\")")
    }

    "no type param" in {
      implicit class Inc(field: Int) {
        def plus(delta: Int) = quote(infix"$field + $delta".as[Long])
      }

      val q = quote(query[TestEntity].map(e => unquote(e.i.plus(10))))
      testContext.run(q).ast.toString must equal("query[TestEntity].map(e => infix\"" + "$" + "{e.i} + " + "$" + "{10}\")")
    }
  }
}
