package io.getquill

import io.getquill.context.Context

trait TestEntities {
  this: Context[_, _] =>

  case class TestEntity(s: String, i: Int, l: Long, o: Option[Int])
  case class TestEntity2(s: String, i: Int, l: Long, o: Option[Int])
  case class TestEntity3(s: String, i: Int, l: Long, o: Option[Int])
  case class TestEntity4(i: Long)
  case class TestEntity5(s: String, i: Long)

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
  val qr5 = quote {
    for {
      a <- query[TestEntity]
    } yield TestEntity5(a.s, a.l)
  }
}
