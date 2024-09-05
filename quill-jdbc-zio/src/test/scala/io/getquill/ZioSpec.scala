package io.getquill

import io.getquill.base.Spec
import io.getquill.context.qzio.ImplicitSyntax._
import org.scalatest.BeforeAndAfterAll
import zio.stream.{ZSink, ZStream}
import zio.{Runtime, Tag, Unsafe, ZEnvironment, ZIO, ZLayer}

import java.sql.Connection
import javax.sql.DataSource

object ZioSpec {
  def runLayerUnsafe[T: Tag](layer: ZLayer[Any, Throwable, T]): T =
    zio.Unsafe.unsafe { implicit u =>
      zio.Runtime.default.unsafe.run(zio.Scope.global.extend(layer.build)).getOrThrow()
    }.get
}

trait ZioSpec extends Spec with BeforeAndAfterAll {

  def accumulate[T](stream: ZStream[Any, Throwable, T]): ZIO[Any, Throwable, List[T]] =
    stream.run(ZSink.collectAll).map(_.toList)

  def collect[T](stream: ZStream[Any, Throwable, T]): List[T] =
    Unsafe.unsafe { implicit u =>
      zio.Runtime.default.unsafe.run(stream.run(ZSink.collectAll).map(_.toList)).getOrThrow()
    }

  def collect[T](qzio: ZIO[Any, Throwable, T]): T =
    Unsafe.unsafe { implicit u =>
      zio.Runtime.default.unsafe.run(qzio).getOrThrow()
    }

  implicit class ZStreamTestExt[T](stream: ZStream[Any, Throwable, T]) {
    def runSyncUnsafe(): List[T] = collect[T](stream)
  }

  implicit class ZioTestExt[T](qzio: ZIO[Any, Throwable, T]) {
    def runSyncUnsafe(): T = collect[T](qzio)
  }
}

trait ZioProxySpec extends Spec with BeforeAndAfterAll {

  def accumulateDS[T](stream: ZStream[DataSource, Throwable, T]): ZIO[DataSource, Throwable, List[T]] =
    stream.run(ZSink.collectAll).map(_.toList)

  def accumulate[T](stream: ZStream[Connection, Throwable, T]): ZIO[Connection, Throwable, List[T]] =
    stream.run(ZSink.collectAll).map(_.toList)

  def collect[T](stream: ZStream[DataSource, Throwable, T])(implicit runtime: Implicit[DataSource]): List[T] =
    Unsafe.unsafe { implicit u =>
      zio.Runtime.default.unsafe
        .run(stream.run(ZSink.collectAll).map(_.toList).provideEnvironment(ZEnvironment(runtime.env)))
        .getOrThrow()
    }

  def collect[T](qzio: ZIO[DataSource, Throwable, T])(implicit runtime: Implicit[DataSource]): T =
    Unsafe.unsafe { implicit u =>
      zio.Runtime.default.unsafe.run(qzio.provideEnvironment(ZEnvironment(runtime.env))).getOrThrow()
    }

  implicit class ZioAnyOps[T](qzio: ZIO[Any, Throwable, T]) {
    def runSyncUnsafe(): T =
      Unsafe.unsafe { implicit u =>
        Runtime.default.unsafe.run(qzio).getOrThrow()
      }
  }

  implicit class ZStreamTestExt[T](stream: ZStream[DataSource, Throwable, T])(implicit runtime: Implicit[DataSource]) {
    def runSyncUnsafe(): List[T] = collect[T](stream)
  }

  implicit class ZioTestExt[T](qzio: ZIO[DataSource, Throwable, T])(implicit runtime: Implicit[DataSource]) {
    def runSyncUnsafe(): T = collect[T](qzio)
  }
}
