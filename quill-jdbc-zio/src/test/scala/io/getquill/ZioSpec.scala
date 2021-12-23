package io.getquill

import io.getquill.context.ZioJdbc.DataSource
import io.getquill.util.LoadConfig
import org.scalatest.BeforeAndAfterAll
import zio.stream.{ Sink, ZStream }
import zio.{ Has, Runtime, ZIO }

import java.sql.Connection

case class Prefix(name: String)

trait ZioSpec extends Spec with BeforeAndAfterAll {
  def prefix: Prefix

  var pool: DataSource = _

  override def beforeAll: Unit = {
    super.beforeAll()
    pool = JdbcContextConfig(LoadConfig(prefix.name)).dataSource
  }

  override def afterAll(): Unit = {
    pool.close()
  }

  def accumulateDS[T](stream: ZStream[Has[DataSource], Throwable, T]): ZIO[Has[DataSource], Throwable, List[T]] =
    stream.run(Sink.collectAll).map(_.toList)

  def accumulate[T](stream: ZStream[Has[Connection], Throwable, T]): ZIO[Has[Connection], Throwable, List[T]] =
    stream.run(Sink.collectAll).map(_.toList)

  def collect[T](stream: ZStream[Has[DataSource], Throwable, T]): List[T] =
    Runtime.default.unsafeRun(stream.run(Sink.collectAll).map(_.toList).provide(Has(pool)))

  def collect[T](qzio: ZIO[Has[DataSource], Throwable, T]): T =
    Runtime.default.unsafeRun(qzio.provide(Has(pool)))

  implicit class ZioAnyOps[T](qzio: ZIO[Any, Throwable, T]) {
    def runSyncUnsafe(): T = Runtime.default.unsafeRun(qzio)
  }

  implicit class ZStreamTestExt[T](stream: ZStream[Has[DataSource], Throwable, T]) {
    def runSyncUnsafe(): List[Any] = collect[T](stream)
  }

  implicit class ZioTestExt[T](qzio: ZIO[Has[DataSource], Throwable, T]) {
    def runSyncUnsafe(): T = collect[T](qzio)
  }
}
