package io.getquill

import org.scalatest.BeforeAndAfterAll
import org.scalatest.FreeSpec
import org.scalatest.MustMatchers

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration

abstract class Spec extends FreeSpec with MustMatchers with BeforeAndAfterAll {
  def await[T](f: Future[T]): T = Await.result(f, Duration.Inf)
}
