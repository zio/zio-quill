package io.getquill

import io.getquill.context.Context

trait TestEntities {
  this: Context[_, _] =>

  case class TestEntity(s: String, i: Int, l: Long, o: Option[Int])
  case class TestEntity2(s: String, i: Int, l: Long, o: Option[Int])
  case class TestEntity3(s: String, i: Int, l: Long, o: Option[Int])

  case class TestEntityEmbedded(s: String) extends Embedded
  case class TestEntityEmbedded2(embedded: TestEntityEmbedded) extends Embedded
  case class TestEntityParent(embedded2: TestEntityEmbedded2)
  val qEmbedded = quote(query[TestEntityParent])

  val qr1 = quote {
    query[TestEntity]
  }
  val qr2 = quote {
    query[TestEntity2]
  }
  val qr3 = quote {
    query[TestEntity3]
  }
}
