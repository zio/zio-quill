package io.getquill

import io.getquill.context.Context

trait TestEntities {
  this: Context[_, _] =>

  case class TestEntity(s: String, i: Int, l: Long, o: Option[Int])
  case class TestEntity2(s: String, i: Int, l: Long, o: Option[Int])
  case class TestEntity3(s: String, i: Int, l: Long, o: Option[Int])
  case class TestEntity4(i: Long)

  val qr1 = quote {
    query[TestEntity]
  }
  val qr2 = quote {
    query[TestEntity2]
  }
  val qr3 = quote {
    query[TestEntity3]
  }
  val qr4 = quote {
    query[TestEntity4]
  }
}
