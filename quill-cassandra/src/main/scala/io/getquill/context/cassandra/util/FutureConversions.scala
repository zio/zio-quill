package io.getquill.context.cassandra.util

import com.datastax.oss.driver.shaded.guava.common.util.concurrent.ListenableFuture

import java.util.concurrent.Executor

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try

object FutureConversions {

  implicit class ListenableFutureConverter[A](val lf: ListenableFuture[A]) extends AnyVal {
    def asScala(implicit ec: ExecutionContext): Future[A] = {
      val promise = Promise[A]()
      lf.addListener(
        new Runnable {
          def run(): Unit = {
            promise.complete(Try(lf.get()))
            ()
          }
        },
        new Executor {
          override def execute(command: Runnable): Unit = ec.execute(command)
        }
      )
      promise.future
    }

    def asScalaWithDefaultGlobal: Future[A] = {
      import scala.concurrent.ExecutionContext.Implicits.global
      asScala(global)
    }
  }

}
