package io.getquill.quotation

import io.getquill.Spec
import io.getquill.testContext._
import io.getquill.testContext

class RebindSpec extends Spec {

  "rebinds non-arg function" in {
    implicit class ReturnId[T](action: Action[T]) {
      def returnId = quote(infix"$action RETURNING ID".as[Action[T]])
    }
    val q = quote {
      query[TestEntity].insert(e => e.i -> lift(1)).returnId
    }
    testContext.run(q).string mustEqual s"""infix"$${querySchema("TestEntity").insert(e => e.i -> ?)} RETURNING ID""""
  }

  "rebinds property arg" in {
    implicit class OptionalCompare(left: Option[Long]) {
      def valid = quote {
        val now = lift(System.currentTimeMillis)
        infix"($left IS NULL OR $left > $now)".as[Boolean]
      }
    }

    case class A(date: Option[Long] = None)
    testContext.run(query[A].filter(a => a.date.valid)).string mustEqual
      s"""querySchema("A").filter(a => infix"($${a.date} IS NULL OR $${a.date} > $${?})").map(a => a.date)"""
  }

  "rebinds function with args and" - {
    "type param" in {
      implicit class Inc(field: Int) {
        def plus[T](delta: T) = quote(infix"$field + $delta".as[T])
      }

      val q = quote(query[TestEntity].map(e => unquote(e.i.plus(10))))
      testContext.run(q).string mustEqual s"""querySchema("TestEntity").map(e => infix"$${e.i} + $${10}")"""
    }

    "no type param" in {
      implicit class Inc(field: Int) {
        def plus(delta: Int) = quote(infix"$field + $delta".as[Long])
      }

      val q = quote(query[TestEntity].map(e => unquote(e.i.plus(10))))
      testContext.run(q).string mustEqual s"""querySchema("TestEntity").map(e => infix"$${e.i} + $${10}")"""
    }
  }
}
