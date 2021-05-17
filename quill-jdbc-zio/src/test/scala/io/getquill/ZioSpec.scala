package io.getquill

import io.getquill.context.ZioJdbc._
import io.getquill.util.LoadConfig
import org.scalatest.BeforeAndAfterAll
import zio.{ Runtime, ZIO }
import zio.stream.{ Sink, ZStream }

import java.io.Closeable
import javax.sql.DataSource

trait ZioSpec extends Spec with BeforeAndAfterAll {
  def prefix: Prefix;

  var pool: DataSource with Closeable = _

  override def beforeAll = {
    super.beforeAll()
    pool = JdbcContextConfig(LoadConfig(prefix.name)).dataSource
  }

  override def afterAll(): Unit = {
    pool.close()
  }

  def accumulate[T](stream: ZStream[QConnection, Throwable, T]): ZIO[QConnection, Throwable, List[T]] =
    stream.run(Sink.collectAll).map(_.toList)

  def collect[T](stream: ZStream[QConnection, Throwable, T]): List[T] =
    Runtime.default.unsafeRun(stream.run(Sink.collectAll).map(_.toList).provideConnectionFrom(pool))

  def collect[T](qzio: ZIO[QConnection, Throwable, T]): T =
    Runtime.default.unsafeRun(qzio.provideConnectionFrom(pool))

  implicit class ZStreamTestExt[T](stream: ZStream[QConnection, Throwable, T]) {
    def runSyncUnsafe() = collect[T](stream)
  }

  implicit class ZioTestExt[T](qzio: ZIO[QConnection, Throwable, T]) {
    def runSyncUnsafe() = collect[T](qzio)
  }
}
