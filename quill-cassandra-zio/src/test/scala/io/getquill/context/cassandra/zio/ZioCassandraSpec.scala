package io.getquill.context.cassandra.zio

import io.getquill.util.LoadConfig
import io.getquill.{ CassandraContextConfig, CassandraZioSession, Spec }
import zio.{ Runtime, ZEnvironment, ZIO }
import zio.stream.{ ZSink, ZStream }

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

  def accumulate[T](stream: ZStream[CassandraZioSession, Throwable, T]): ZIO[CassandraZioSession, Throwable, List[T]] =
    stream.run(ZSink.collectAll).map(_.toList)

  def result[T](stream: ZStream[CassandraZioSession, Throwable, T]): List[T] =
    Runtime.default.unsafeRun(stream.run(ZSink.collectAll).map(_.toList).provideEnvironment(ZEnvironment(pool)))

  def result[T](qzio: ZIO[CassandraZioSession, Throwable, T]): T =
    Runtime.default.unsafeRun(qzio.provideEnvironment(ZEnvironment(pool)))

  implicit class ZStreamTestExt[T](stream: ZStream[CassandraZioSession, Throwable, T]) {
    def runSyncUnsafe() = result[T](stream)
  }

  implicit class ZioTestExt[T](qzio: ZIO[CassandraZioSession, Throwable, T]) {
    def runSyncUnsafe() = result[T](qzio)
  }
}
