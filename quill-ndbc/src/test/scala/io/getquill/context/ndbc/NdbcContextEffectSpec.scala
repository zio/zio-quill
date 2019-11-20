package io.getquill.context.ndbc

import scala.concurrent.duration.Duration

import io.getquill.Spec
import io.trane.future.scala.{ Await, Future }

class NdbcContextEffectSpec extends Spec {

  def get[T](f: Future[T]): T = Await.result(f, Duration.Inf)

  import NdbcContextEffect._

  "evaluates simple values" in {
    val future = wrap("He-man")
    get(future) mustEqual "He-man"
  }

  "encapsulates exception throw" in {
    val future = wrap(throw new RuntimeException("Surprise!")).failed
    get(future).getMessage mustEqual "Surprise!"
  }

  "pushes an effect correctly" in {
    get(push(Future(1))(_ + 1)) mustEqual 2
  }

  "converts a sequence correctly" in {
    get(seq(List(Future(1), Future(2), Future(3)))) mustEqual List(1, 2, 3)
  }
}