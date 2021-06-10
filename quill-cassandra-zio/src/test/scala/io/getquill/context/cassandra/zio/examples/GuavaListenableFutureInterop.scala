package io.getquill.context.cassandra.zio.examples

import com.google.common.util.concurrent.ListenableFuture
import io.getquill.CassandraZioContext.CIO
import io.getquill.CassandraZioSession
import zio.{ Has, ZIO }
import zio.blocking.Blocking

import java.util.concurrent.Executor
import scala.concurrent.Promise
import scala.util.Try

/**
 * Example of how to directly convert from a Guava ListenableFuture to a ZIO Task (a CIO for Quill Cassandra).
 * Can revert to this if zio/interop-guava needs to be removed.
 */
object GuavaListenableFutureInterop {
  implicit class ZioTaskConverter[A](val lf: ListenableFuture[A]) extends AnyVal {
    def asCio: CIO[A] = {
      def makePromise(executor: Executor): CIO[A] = {
        val promise = Promise[A]()
        lf.addListener(new Runnable {
          def run(): Unit = {
            promise.complete(Try {
              val result = lf.get()
              result
            })
            ()
          }
        }, executor)
        ZIO.fromPromiseScala(promise)
      }

      for {
        env <- ZIO.environment[Has[CassandraZioSession]]
        promise <- makePromise(Blocking.Service.live.blockingExecutor.asJava)
      } yield promise
    }
  }
}
