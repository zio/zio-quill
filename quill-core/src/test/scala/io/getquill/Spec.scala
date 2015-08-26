package io.getquill

import org.scalatest.BeforeAndAfterAll
import org.scalatest.FreeSpec
import org.scalatest.MustMatchers

trait Spec extends FreeSpec with MustMatchers with BeforeAndAfterAll {
  case class TestEntity(s: String, i: Int, l: Long)
  case class TestEntity2(s: String, i: Int, l: Long)
  case class TestEntity3(s: String, i: Int, l: Long)

  val qr1 = quote {
    queryable[TestEntity]
  }
  val qr2 = quote {
    queryable[TestEntity2]
  }
  val qr3 = quote {
    queryable[TestEntity3]
  }
}
