package io.getquill.context.monix

import scala.concurrent.duration.Duration
import io.getquill.Spec
import io.trane.future.scala.{ Await, Future }
import io.getquill.ndbc.TraneFutureConverters._
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global

import scala.util.Try

class MonixNdbcContextEffectSpec extends Spec {
  import MonixNdbcContext.ContextEffect._

  def await[T](t: Task[T]): T = {
    val f: Future[T] = t.runToFuture
    Await.result[T](f, Duration.Inf)
  }

  "evaluates simple values" in {
    val task = wrap("He-man")
    await(task) mustEqual "He-man"
  }

  "evaluates asynchronous values" in {
    await(wrapAsync[String] { doComplete =>
      Future { Thread.sleep(100) }.onComplete { _ =>
        doComplete(Try("hello"))
        ()
      }
    }) mustEqual "hello"
  }

  "encapsulates exception throw" in {
    val task = wrap(throw new RuntimeException("Surprise!")).failed
    await(task).getMessage mustEqual "Surprise!"
  }

  "pushes an effect correctly" in {
    await(push(Task(1))(_ + 1)) mustEqual 2
  }

  "executes effects in sequence" in {
    await(wrap(2).flatMap { prev => wrap(prev + 3) }) mustEqual 5
  }

  "converts a sequence correctly" in {
    await(seq(List(Task(1), Task(2), Task(3)))) mustEqual List(1, 2, 3)
  }

  "converts to Scala Future correctly" in {
    Await.result(toFuture(Task("hello"), monix.execution.Scheduler.Implicits.global), Duration.Inf) mustEqual "hello"
  }

  "creates Future from deferred Future" in {
    val f = fromDeferredFuture((_) => Future.successful("hello"))
    await(f) mustEqual "hello"
  }

  "runs blockingly" in {
    runBlocking(Task { Thread.sleep(100); "foo" }, Duration.Inf) mustEqual "foo"
  }
}
