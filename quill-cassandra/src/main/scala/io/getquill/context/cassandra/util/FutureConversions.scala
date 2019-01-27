package io.getquill.context.cassandra.util

import java.util.concurrent.Executor

import com.google.common.util.concurrent.ListenableFuture

import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.util.Try

object FutureConversions {

  implicit class ListenableFutureConverter[A](val lf: ListenableFuture[A]) extends AnyVal {
    def asScala(implicit ec: ExecutionContext): Future[A] = {
      val promise = Promise[A]
      lf.addListener(new Runnable {
        def run(): Unit = {
          promise.complete(Try(lf.get()))
          ()
        }
      }, ec.asInstanceOf[Executor])
      promise.future
    }

    def asScalaWithDefaultGlobal: Future[A] = {
      import scala.concurrent.ExecutionContext.Implicits.global
      asScala(global)
    }
  }

}
