package io.getquill.context.zio.jasync.postgres

import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import io.getquill.{ Literal, PostgresDialect, Spec }
import io.getquill.context.zio.{ ZIOConnectedContext, ZIOJAsyncConnection }
import org.scalatest.BeforeAndAfterAll
import zio.stream.{ Sink, ZStream }
import zio.{ Has, Runtime, Task, ZIO }

trait ZioSpec extends Spec with BeforeAndAfterAll {

  lazy val context = testContext
  val ctx = context

  def accumulate[T](stream: ZStream[Has[ZIOJAsyncConnection], Throwable, T]): ZIO[Has[ZIOJAsyncConnection], Throwable, List[T]] =
    stream.run(Sink.collectAll).map(_.toList)

  def collect[T](stream: ZStream[Has[ZIOJAsyncConnection], Throwable, T]): List[T] =
    Runtime.default.unsafeRun(stream.run(Sink.collectAll).map(_.toList).provideLayer(testContext.layer))

  def runSyncUnsafe[T](qzio: ZIO[Has[ZIOJAsyncConnection], Throwable, T]): T =
    Runtime.default.unsafeRun(qzio.provideLayer(testContext.layer))

  def runSyncUnsafeWithConnected[T](action: ZIOConnectedContext[PostgresDialect, Literal.type, PostgreSQLConnection] => Task[T]): T =
    Runtime.default.unsafeRun(ZIO.accessM[Has[ZIOConnectedContext[PostgresDialect, Literal.type, PostgreSQLConnection]]](
      env => action(env.get)
    ).provideLayer(testContext.connected))

  implicit class ZioAnyOps[T](qzio: ZIO[Any, Throwable, T]) {
    def runSyncUnsafe() = Runtime.default.unsafeRun(qzio)
  }

  implicit class ZStreamTestExt[T](stream: ZStream[Has[ZIOJAsyncConnection], Throwable, T]) {
    def runSyncUnsafe() = collect[T](stream)
  }

  implicit class ZioTestExt[T](qzio: ZIO[Has[ZIOJAsyncConnection], Throwable, T]) {
    def runSyncUnsafe() = ZioSpec.this.runSyncUnsafe[T](qzio)
  }
}
