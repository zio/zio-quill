package io.getquill.quotation

import io.getquill._
import io.getquill.TestSource.mirrorSource

class RebindSpec extends Spec {
  "rebind non-arg function" - {
    "with type param" in {
      implicit class ReturnId[T](action: Action[T]) {
        def returnId[ID] = quote(infix"$action RETURNING ID".as[Action[ID]])
      }
      val q = quote { (i: Int) =>
        unquote(query[TestEntity].insert(e => e.i -> i).returnId[Long])
      }
      mirrorSource.run(q)(List(1)).ast.toString must equal("infix\"" + "$" + "{query[TestEntity].insert(e => e.i -> p1)} RETURNING ID\"")
    }

    "with no type param" in {
      implicit class ReturnId[T](action: Action[T]) {
        def returnId = quote(infix"$action RETURNING ID".as[Action[Long]])
      }
      val q = quote { (i: Int) =>
        unquote(query[TestEntity].insert(e => e.i -> i).returnId)
      }
      mirrorSource.run(q)(List(1)).ast.toString must equal("infix\"" + "$" + "{query[TestEntity].insert(e => e.i -> p1)} RETURNING ID\"")
    }
  }

  "rebind function with args and" - {
    "type param" in {
      implicit class Inc(field: Int) {
        def plus[T](delta: T) = quote(infix"$field + $delta".as[T])
      }

      val q = quote(query[TestEntity].map(e => unquote(e.i.plus(10))))
      mirrorSource.run(q).ast.toString must equal("query[TestEntity].map(e => infix\"" + "$" + "{e.i} + " + "$" + "{10}\")")
    }

    "no type param" in {
      implicit class Inc(field: Int) {
        def plus(delta: Int) = quote(infix"$field + $delta".as[Long])
      }

      val q = quote(query[TestEntity].map(e => unquote(e.i.plus(10))))
      mirrorSource.run(q).ast.toString must equal("query[TestEntity].map(e => infix\"" + "$" + "{e.i} + " + "$" + "{10}\")")
    }
  }
}
