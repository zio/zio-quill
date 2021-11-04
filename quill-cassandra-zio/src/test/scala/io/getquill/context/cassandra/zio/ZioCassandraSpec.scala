package io.getquill.context.cassandra.zio

import io.getquill.util.LoadConfig
import io.getquill.{ CassandraContextConfig, CassandraZioSession, Spec }
import zio.{ Has, Runtime, ZIO }
import zio.stream.{ Sink, ZStream }

trait ZioCassandraSpec extends Spec {

  var pool: CassandraZioSession = _

  override def beforeAll = {
    super.beforeAll()
    val config = CassandraContextConfig(LoadConfig("testStreamDB"))
    pool = CassandraZioSession(config.session, config.preparedStatementCacheSize)

  }

  override def afterAll(): Unit = {
    pool.close()
  }

  def accumulate[T](stream: ZStream[Has[CassandraZioSession], Throwable, T]): ZIO[Has[CassandraZioSession], Throwable, List[T]] =
    stream.run(Sink.collectAll).map(_.toList)

  def result[T](stream: ZStream[Has[CassandraZioSession], Throwable, T]): List[T] =
    Runtime.default.unsafeRun(stream.run(Sink.collectAll).map(_.toList).provide(Has(pool)))

  def result[T](qzio: ZIO[Has[CassandraZioSession], Throwable, T]): T =
    Runtime.default.unsafeRun(qzio.provide(Has(pool)))

  implicit class ZStreamTestExt[T](stream: ZStream[Has[CassandraZioSession], Throwable, T]) {
    def runSyncUnsafe() = result[T](stream)
  }

  implicit class ZioTestExt[T](qzio: ZIO[Has[CassandraZioSession], Throwable, T]) {
    def runSyncUnsafe() = result[T](qzio)
  }
}
