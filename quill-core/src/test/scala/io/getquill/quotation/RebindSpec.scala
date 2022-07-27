package io.getquill.quotation

import io.getquill.testContext._
import io.getquill.testContext
import io.getquill.Action
import io.getquill.base.Spec

class RebindSpec extends Spec {

  "rebinds non-arg function" in {
    implicit class ReturnId[T](action: Action[T]) {
      def returnId = quote(sql"$action RETURNING ID".as[Action[T]])
    }
    val q = quote {
      query[TestEntity].insert(e => e.i -> lift(1)).returnId
    }
    testContext.run(q).string mustEqual s"""sql"$${querySchema("TestEntity").insert(e => e.i -> ?)} RETURNING ID""""
  }

  "rebinds property arg" in {
    implicit class OptionalCompare(left: Option[Long]) {
      def valid = quote {
        val now = lift(System.currentTimeMillis)
        sql"($left IS NULL OR $left > $now)".as[Boolean]
      }
    }

    case class A(date: Option[Long] = None)
    testContext.run(query[A].filter(a => a.date.valid)).string mustEqual
      s"""querySchema("A").filter(a => sql"($${a.date} IS NULL OR $${a.date} > $${?})")"""
  }

  "rebinds function with args and" - {
    "type param" in {
      implicit class Inc(field: Int) {
        def plus[T](delta: T) = quote(sql"$field + $delta".as[T])
      }

      val q = quote(query[TestEntity].map(e => unquote(e.i.plus(10))))
      testContext.run(q).string mustEqual s"""querySchema("TestEntity").map(e => sql"$${e.i} + $${10}")"""
    }

    "no type param" in {
      implicit class Inc(field: Int) {
        def plus(delta: Int) = quote(sql"$field + $delta".as[Long])
      }

      val q = quote(query[TestEntity].map(e => unquote(e.i.plus(10))))
      testContext.run(q).string mustEqual s"""querySchema("TestEntity").map(e => sql"$${e.i} + $${10}")"""
    }
  }
}
