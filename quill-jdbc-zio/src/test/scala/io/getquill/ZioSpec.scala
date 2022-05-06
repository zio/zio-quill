package io.getquill

import io.getquill.context.qzio.ImplicitSyntax._
import org.scalatest.BeforeAndAfterAll
import zio.stream.{ ZSink, ZStream }
import zio.{ Runtime, ZIO }

import java.sql.Connection
import javax.sql.DataSource

trait ZioSpec extends Spec with BeforeAndAfterAll {

  def accumulateDS[T](stream: ZStream[DataSource, Throwable, T]): ZIO[DataSource, Throwable, List[T]] =
    stream.run(ZSink.collectAll).map(_.toList)

  def accumulate[T](stream: ZStream[Connection, Throwable, T]): ZIO[Connection, Throwable, List[T]] =
    stream.run(ZSink.collectAll).map(_.toList)

  def collect[T](stream: ZStream[DataSource, Throwable, T])(implicit runtime: Implicit[Runtime.Scoped[DataSource]]): List[T] =
    runtime.env.unsafeRun(stream.run(ZSink.collectAll).map(_.toList))

  def collect[T](qzio: ZIO[DataSource, Throwable, T])(implicit runtime: Implicit[Runtime.Scoped[DataSource]]): T =
    runtime.env.unsafeRun(qzio)

  // TODO Change to runUnsafe
  implicit class ZioAnyOps[T](qzio: ZIO[Any, Throwable, T]) {
    def runSyncUnsafe() = Runtime.default.unsafeRun(qzio)
  }

  implicit class ZStreamTestExt[T](stream: ZStream[DataSource, Throwable, T])(implicit runtime: Implicit[Runtime.Scoped[DataSource]]) {
    def runSyncUnsafe() = collect[T](stream)
  }

  implicit class ZioTestExt[T](qzio: ZIO[DataSource, Throwable, T])(implicit runtime: Implicit[Runtime.Scoped[DataSource]]) {
    def runSyncUnsafe() = collect[T](qzio)
  }
}
