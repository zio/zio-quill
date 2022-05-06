package io.getquill.context.zio.jasync.postgres

import io.getquill.Spec
import io.getquill.context.zio.ZioJAsyncConnection
import org.scalatest.BeforeAndAfterAll
import zio.stream.{ ZSink, ZStream }
import zio.{ Runtime, ZIO }

trait ZioSpec extends Spec with BeforeAndAfterAll {

  lazy val context = testContext
  val ctx = context

  def accumulate[T](stream: ZStream[ZioJAsyncConnection, Throwable, T]): ZIO[ZioJAsyncConnection, Throwable, List[T]] =
    stream.run(ZSink.collectAll).map(_.toList)

  def collect[T](stream: ZStream[ZioJAsyncConnection, Throwable, T]): List[T] =
    Runtime.default.unsafeRun(stream.run(ZSink.collectAll).map(_.toList).provideLayer(testContext.layer))

  def runSyncUnsafe[T](qzio: ZIO[ZioJAsyncConnection, Throwable, T]): T =
    Runtime.default.unsafeRun(qzio.provideLayer(testContext.layer))

  implicit class ZioAnyOps[T](qzio: ZIO[Any, Throwable, T]) {
    def runSyncUnsafe() = Runtime.default.unsafeRun(qzio)
  }

  implicit class ZStreamTestExt[T](stream: ZStream[ZioJAsyncConnection, Throwable, T]) {
    def runSyncUnsafe() = collect[T](stream)
  }

  implicit class ZioTestExt[T](qzio: ZIO[ZioJAsyncConnection, Throwable, T]) {
    def runSyncUnsafe() = ZioSpec.this.runSyncUnsafe[T](qzio)
  }
}
