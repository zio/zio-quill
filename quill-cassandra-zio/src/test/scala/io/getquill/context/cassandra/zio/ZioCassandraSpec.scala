package io.getquill.context.cassandra.zio

import io.getquill.CassandraZioContext._
import io.getquill.util.LoadConfig
import io.getquill.{ CassandraContextConfig, Spec, CassandraZioSession }
import zio.{ Runtime, ZIO }
import zio.stream.{ Sink, ZStream }

trait ZioCassandraSpec extends Spec {

  var pool: CassandraZioSession = _

  override def beforeAll = {
    super.beforeAll()
    val config = CassandraContextConfig(LoadConfig("testStreamDB"))
    pool = CassandraZioSession(config.cluster, config.keyspace, config.preparedStatementCacheSize)

  }

  override def afterAll(): Unit = {
    pool.close()
  }

  def accumulate[T](stream: ZStream[BlockingSession, Throwable, T]): ZIO[BlockingSession, Throwable, List[T]] =
    stream.run(Sink.collectAll).map(_.toList)

  def result[T](stream: ZStream[BlockingSession, Throwable, T]): List[T] =
    Runtime.default.unsafeRun(stream.run(Sink.collectAll).map(_.toList).provideSession(pool))

  def result[T](qzio: ZIO[BlockingSession, Throwable, T]): T =
    Runtime.default.unsafeRun(qzio.provideSession(pool))

  implicit class ZStreamTestExt[T](stream: ZStream[BlockingSession, Throwable, T]) {
    def runSyncUnsafe() = result[T](stream)
  }

  implicit class ZioTestExt[T](qzio: ZIO[BlockingSession, Throwable, T]) {
    def runSyncUnsafe() = result[T](qzio)
  }
}
