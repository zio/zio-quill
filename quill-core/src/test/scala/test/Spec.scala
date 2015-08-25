package test

import io.getquill._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FreeSpec
import org.scalatest.Inside
import org.scalatest.MustMatchers

trait Spec extends FreeSpec with MustMatchers with Inside with BeforeAndAfterAll {
  case class TestEntity(s: String)
  case class TestEntity2(s: String)
  case class TestEntity3(s: String)

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
