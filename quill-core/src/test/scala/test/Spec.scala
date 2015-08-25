package test

import org.scalatest.BeforeAndAfterAll
import org.scalatest.FreeSpec
import org.scalatest.Inside
import org.scalatest.MustMatchers

trait Spec extends FreeSpec with MustMatchers with Inside with BeforeAndAfterAll {
  case class TestEntity(s: String)
  case class TestEntity2(s: String)
  case class TestEntity3(s: String)
}
