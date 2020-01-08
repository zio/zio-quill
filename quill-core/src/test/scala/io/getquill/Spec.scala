package io.getquill

import org.scalatest.BeforeAndAfterAll
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }

abstract class Spec extends AnyFreeSpec with Matchers with BeforeAndAfterAll {
  def await[T](f: Future[T]): T = Await.result(f, Duration.Inf)
}
