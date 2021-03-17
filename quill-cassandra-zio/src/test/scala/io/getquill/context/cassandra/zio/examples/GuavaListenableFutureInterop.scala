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
      def makePromise(ec: Executor): CIO[A] = {
        val promise = Promise[A]()
        lf.addListener(new Runnable {
          def run(): Unit = {
            promise.complete(Try {
              val result = lf.get()
              result
            })
            ()
          }
        }, ec.asInstanceOf[Executor])
        ZIO.fromPromiseScala(promise)
      }

      for {
        env <- ZIO.environment[Has[CassandraZioSession] with Blocking]
        block = env.get[Blocking.Service]
        promise <- makePromise(block.blockingExecutor.asJava)
      } yield promise
    }
  }
}
