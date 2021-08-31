package io.getquill.util

import com.google.common.util.concurrent.{ FutureCallback, Futures, ListenableFuture }
import cats.effect.Async

import cats.implicits._
import com.datastax.driver.core.{ ResultSet, ResultSetFuture }

import java.util.concurrent.Executor
import scala.concurrent.ExecutionContext
import scala.language.higherKinds

object GuavaCeUtils {

  case class ecExecutor(ec: ExecutionContext) extends Executor {
    def execute(command: Runnable): Unit = ec.execute(command)
  }

  implicit class RichResultSetFuture[F[_]: Async](rsf: F[ResultSetFuture]) {
    def toAsync: F[ResultSet] = (Async[F].executionContext product rsf) flatMap {
      case (ec, lf) =>
        Async[F].async_ { cb =>
          Futures.addCallback(lf, new FutureCallback[ResultSet] {
            override def onFailure(t: Throwable): Unit = cb(Left(t))

            override def onSuccess(result: ResultSet): Unit = cb(Right(result))
          }, ecExecutor(ec))
        }
    }
  }

  implicit class RichListenableFuture[T, F[_]: Async](flf: F[ListenableFuture[T]]) {

    def toAsync: F[T] = (Async[F].executionContext product flf) flatMap {
      case (ec, lf) =>
        Async[F].async_ { cb =>
          Futures.addCallback(lf, new FutureCallback[T] {
            override def onFailure(t: Throwable): Unit = cb(Left(t))

            override def onSuccess(result: T): Unit = cb(Right(result))
          }, ecExecutor(ec))
        }
    }
  }
}
