package io.getquill

import io.getquill.util.LoadConfig
import org.scalatest.BeforeAndAfterAll
import zio.{ Has, Runtime, ZIO }
import zio.stream.{ Sink, ZStream }
import io.getquill.context.ZioJdbc._

import java.io.Closeable
import java.sql.Connection
import javax.sql.DataSource

case class Prefix(name: String)

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

  def accumulate[T](stream: ZStream[Has[Connection], Throwable, T]): ZIO[Has[Connection], Throwable, List[T]] =
    stream.run(Sink.collectAll).map(_.toList)

  def collect[T](stream: ZStream[Has[Connection], Throwable, T]): List[T] =
    Runtime.default.unsafeRun(stream.run(Sink.collectAll).map(_.toList).onDataSource.provide(Has(pool)))

  def collect[T](qzio: ZIO[Has[Connection], Throwable, T]): T =
    Runtime.default.unsafeRun(qzio.onDataSource.provide(Has(pool)))

  implicit class ZioAnyOps[T](qzio: ZIO[Any, Throwable, T]) {
    def runSyncUnsafe() = Runtime.default.unsafeRun(qzio)
  }

  implicit class ZStreamTestExt[T](stream: ZStream[Has[Connection], Throwable, T]) {
    def runSyncUnsafe() = collect[T](stream)
  }

  implicit class ZioTestExt[T](qzio: ZIO[Has[Connection], Throwable, T]) {
    def runSyncUnsafe() = collect[T](qzio)
  }
}
