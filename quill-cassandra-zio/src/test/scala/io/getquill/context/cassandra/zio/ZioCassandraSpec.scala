package io.getquill.context.cassandra.zio

import io.getquill.base.Spec
import zio.stream.{ZSink, ZStream}
import zio.{Runtime, Tag, Unsafe, ZEnvironment, ZIO, ZLayer}

object ZioCassandraSpec {
  def runLayerUnsafe[T: Tag](layer: ZLayer[Any, Throwable, T]): T =
    zio.Unsafe.unsafe { implicit u =>
      zio.Runtime.default.unsafe.run(zio.Scope.global.extend(layer.build)).getOrThrow()
    }.get
}

trait ZioCassandraSpec extends Spec {
  def accumulate[T](stream: ZStream[Any, Throwable, T]): ZIO[Any, Throwable, List[T]] =
    stream.run(ZSink.collectAll).map(_.toList)

  def result[T](stream: ZStream[Any, Throwable, T]): List[T] =
    Unsafe.unsafe { implicit u =>
      Runtime.default.unsafe
        .run(stream.run(ZSink.collectAll).map(_.toList).provideEnvironment(ZEnvironment(pool)))
        .getOrThrow()
    }

  def result[T](qzio: ZIO[Any, Throwable, T]): T =
    Unsafe.unsafe { implicit u =>
      Runtime.default.unsafe.run(qzio.provideEnvironment(ZEnvironment(pool))).getOrThrow()
    }

  implicit class ZStreamTestExt[T](stream: ZStream[Any, Throwable, T]) {
    def runSyncUnsafe() = result[T](stream)
  }

  implicit class ZioTestExt[T](qzio: ZIO[Any, Throwable, T]) {
    def runSyncUnsafe() = result[T](qzio)
  }
}
