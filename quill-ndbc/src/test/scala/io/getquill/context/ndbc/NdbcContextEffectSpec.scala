package io.getquill.context.ndbc

import scala.concurrent.duration.Duration
import io.getquill.Spec
import io.trane.future.scala.{ Await, Future, Promise }

import scala.util.Try

class NdbcContextEffectSpec extends Spec {

  def get[T](f: Future[T]): T = Await.result(f, Duration.Inf)

  import NdbcContext.ContextEffect._

  "evaluates simple values" in {
    val future = wrap("He-man")
    get(future) mustEqual "He-man"
  }

  "evaluates asynchronous values" in {
    get(wrapAsync[String] { doComplete =>
      Future { Thread.sleep(100) }.onComplete { _ =>
        doComplete(Try("hello"))
        ()
      }
    }) mustEqual "hello"
  }

  "encapsulates exception throw" in {
    val future = wrap(throw new RuntimeException("Surprise!")).failed
    get(future).getMessage mustEqual "Surprise!"
  }

  "pushes an effect correctly" in {
    get(push(Future(1))(_ + 1)) mustEqual 2
  }

  "executes effects in sequence" in {
    get(wrap(2).flatMap { prev => wrap(prev + 3) }) mustEqual 5
  }

  "converts a sequence correctly" in {
    get(seq(List(Future(1), Future(2), Future(3)))) mustEqual List(1, 2, 3)
  }

  "converts to Scala Future correctly" in {
    val p = Promise[Unit]()
    val f = toFuture(p.future, ())

    f.isCompleted mustEqual false
    p.complete(Try(()))
    f.isCompleted mustEqual true
  }

  "creates Future from deferred Future" in {
    val f = fromDeferredFuture((_) => Future.successful("hello"))
    get(f) mustEqual "hello"
  }

  "runs blockingly" in {
    runBlocking(Future { Thread.sleep(100); "foo" }, Duration.Inf) mustEqual "foo"
  }
}