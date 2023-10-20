package io.getquill.cassandrazio

import com.datastax.oss.driver.api.core.cql.{BoundStatement, Row}
import io.getquill.context.cassandra.{CassandraRowContext, CqlIdiom}
import io.getquill.context.qzio.ZioContext
import io.getquill.context.{Context, ExecutionInfo}
import io.getquill.util.ContextLogger
import io.getquill.{CassandraZioContext, NamingStrategy}
import zio.stream.ZStream
import zio.{Tag, ZEnvironment, ZIO, ZLayer}

object Quill {

  type CassandraZioSession = io.getquill.CassandraZioSession
  val CassandraZioSession = io.getquill.CassandraZioSession

  object Cassandra {
    def fromNamingStrategy[N <: NamingStrategy: Tag](naming: N): ZLayer[CassandraZioSession, Nothing, Cassandra[N]] =
      ZLayer.fromFunction((session: CassandraZioSession) => new Cassandra[N](naming, session))
  }

  class Cassandra[+N <: NamingStrategy](val naming: N, session: CassandraZioSession)
      extends CassandraRowContext[N]
      with ZioContext[CqlIdiom, N]
      with Context[CqlIdiom, N]
      with Probing {

    override type Error       = Throwable
    override type Environment = Any

    override type StreamResult[T] = ZStream[Any, Throwable, T]
    override type RunActionResult = Unit
    override type Result[T]       = ZIO[Any, Throwable, T]

    override type RunQueryResult[T]       = List[T]
    override type RunQuerySingleResult[T] = T
    override type RunBatchActionResult    = Unit

    override type PrepareRow = BoundStatement
    override type ResultRow  = Row
    override type Session    = CassandraZioSession

    val underlying: CassandraZioContext[N] = new CassandraZioContext[N](naming)

    def streamQuery[T](
      fetchSize: Option[Int],
      cql: String,
      prepare: Prepare = identityPrepare,
      extractor: Extractor[T] = identityExtractor
    )(info: ExecutionInfo, dc: Runner): ZStream[Any, Throwable, T] =
      onSessionStream(underlying.streamQuery(fetchSize, cql, prepare, extractor)(info, dc))
    def executeQuery[T](cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(
      info: ExecutionInfo,
      dc: Runner
    ): ZIO[Any, Throwable, List[T]] =
      onSession(underlying.executeQuery(cql, prepare, extractor)(info, dc))

    def executeQuerySingle[T](
      cql: String,
      prepare: Prepare = identityPrepare,
      extractor: Extractor[T] = identityExtractor
    )(info: ExecutionInfo, dc: Runner): ZIO[Any, Throwable, T] =
      onSession(underlying.executeQuerySingle(cql, prepare, extractor)(info, dc))

    def executeAction(cql: String, prepare: Prepare = identityPrepare)(
      info: ExecutionInfo,
      dc: Runner
    ): ZIO[Any, Throwable, Unit] =
      onSession(underlying.executeAction(cql, prepare)(info, dc))

    def executeBatchAction(groups: List[BatchGroup])(info: ExecutionInfo, dc: Runner): ZIO[Any, Throwable, Unit] =
      onSession(underlying.executeBatchAction(groups.asInstanceOf[List[this.underlying.BatchGroup]])(info, dc))

    private def onSession[T](zio: ZIO[CassandraZioSession, Throwable, T]) =
      zio.provideEnvironment(ZEnvironment(session))

    private def onSessionStream[T](zio: ZStream[CassandraZioSession, Throwable, T]) =
      zio.provideEnvironment(ZEnvironment(session))

    override def close() = session.close()
  }
}
