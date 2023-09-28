package io.getquill.context.qzio.jasync.postgres

import io.getquill.base.Spec
import io.getquill.context.qzio.ZioJAsyncConnection
import org.scalatest.BeforeAndAfterAll
import zio.stream.{ZSink, ZStream}
import zio.{Runtime, Unsafe, ZIO}

trait ZioSpec extends Spec with BeforeAndAfterAll {

  lazy val context = testContext
  val ctx          = context

  def accumulate[T](stream: ZStream[ZioJAsyncConnection, Throwable, T]): ZIO[ZioJAsyncConnection, Throwable, List[T]] =
    stream.run(ZSink.collectAll).map(_.toList)

  def collect[T](stream: ZStream[ZioJAsyncConnection, Throwable, T]): List[T] =
    Unsafe.unsafe { implicit u =>
      Runtime.default.unsafe
        .run(stream.run(ZSink.collectAll).map(_.toList).provideLayer(testContext.layer))
        .getOrThrow()
    }

  def runSyncUnsafe[T](qzio: ZIO[ZioJAsyncConnection, Throwable, T]): T =
    Unsafe.unsafe { implicit u =>
      Runtime.default.unsafe.run(qzio.provideLayer(testContext.layer)).getOrThrow()
    }

  implicit class ZioAnyOps[T](qzio: ZIO[Any, Throwable, T]) {
    def runSyncUnsafe(): T =
      Unsafe.unsafe { implicit u =>
        Runtime.default.unsafe.run(qzio).getOrThrow()
      }
  }

  implicit class ZStreamTestExt[T](stream: ZStream[ZioJAsyncConnection, Throwable, T]) {
    def runSyncUnsafe(): List[T] = collect[T](stream)
  }

  implicit class ZioTestExt[T](qzio: ZIO[ZioJAsyncConnection, Throwable, T]) {
    def runSyncUnsafe(): T = ZioSpec.this.runSyncUnsafe[T](qzio)
  }
}
