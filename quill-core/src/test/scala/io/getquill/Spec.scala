package io.getquill

import org.scalatest.BeforeAndAfterAll
import org.scalatest.FreeSpec
import org.scalatest.MustMatchers

trait Spec extends FreeSpec with MustMatchers with BeforeAndAfterAll {

  System.setProperty("mirrorSource.testKey", "testValue")

  case class WrappedLong(l: Long)
  object WrappedLong {
    implicit val ordering = Ordering.fromLessThan[WrappedLong](_.l < _.l)
  }

  case class WrappedInt(i: Int)
  object WrappedInt {
    implicit val numeric = new Numeric[WrappedInt] {
      override def plus(x: WrappedInt, y: WrappedInt): WrappedInt = WrappedInt(x.i + y.i)
      override def times(x: WrappedInt, y: WrappedInt): WrappedInt = WrappedInt(x.i * y.i)
      override def minus(x: WrappedInt, y: WrappedInt): WrappedInt = WrappedInt(x.i - y.i)
      override def compare(x: WrappedInt, y: WrappedInt): Int = x.i.compare(y.i)
      override def toDouble(x: WrappedInt): Double = x.i.toDouble
      override def toFloat(x: WrappedInt): Float = x.i.toFloat
      override def fromInt(x: Int): WrappedInt = WrappedInt(x)
      override def toInt(x: WrappedInt): Int =  x.i
      override def negate(x: WrappedInt): WrappedInt = WrappedInt(-x.i)
      override def toLong(x: WrappedInt): Long = x.toLong
    }
  }

  case class TestEntity(s: String, i: Int, l: Long, o: Option[Int])
  case class TestEntity2(s: String, i: Int, l: Long, o: Option[Int])
  case class TestEntity3(s: String, i: Int, l: Long, o: Option[Int])
  case class TestEntity4(s: String, i: WrappedInt, l: WrappedLong, o: Option[Int])

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
