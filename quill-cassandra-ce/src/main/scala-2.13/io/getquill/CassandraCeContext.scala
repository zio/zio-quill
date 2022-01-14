package io.getquill

import cats._
import cats.effect._
import com.datastax.oss.driver.api.core.cql.Row
import cats.syntax.all._
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.AsyncResultSet
import fs2.{ Chunk, Stream }
import com.typesafe.config.Config
import io.getquill.context.cassandra.CqlIdiom
import io.getquill.util.{ ContextLogger, LoadConfig }
import io.getquill.context.ExecutionInfo
import io.getquill.context.ce.CeContext

import scala.jdk.CollectionConverters._
import scala.language.higherKinds

class CassandraCeContext[N <: NamingStrategy, F[_]](
                                                     naming:                     N,
                                                     session:                    CqlSession,
                                                     preparedStatementCacheSize: Long
                                                   )(implicit val af: Async[F])
  extends CassandraCqlSessionContext[N](naming, session, preparedStatementCacheSize)
    with CeContext[CqlIdiom, N, F] {

  private val logger = ContextLogger(classOf[CassandraCeContext[_, F]])

  private[getquill] def prepareRowAndLog(cql: String, prepare: Prepare = identityPrepare): F[PrepareRow] = for {
    ec <- Async[F].executionContext
    futureStatement = Sync[F].delay(prepareAsync(cql)(ec))
    prepStatement <- Async[F].fromFuture(futureStatement)
    (params, bs) = prepare(prepStatement, this)
    _ <- Sync[F].delay(logger.logQuery(cql, params))
  } yield bs

  protected def page(rs: AsyncResultSet): Stream[F, Row] =
    Stream.unfoldChunkEval(rs.remaining())(rem =>
      if (rem > 0)
        af.delay[Option[(Chunk[Row], Int)]] {
          val chunk: Chunk[Row] = Chunk.iterable(rs.currentPage().asScala)
          Some((chunk, rs.remaining()))
        }
      else
        af.pure[Option[(Chunk[Row], Int)]](None))

  def streamQuery[T](cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(info: ExecutionInfo, dc: Runner): StreamResult[T] = {
    Stream
      .eval(prepareRowAndLog(cql, prepare))
      .evalMap(p => af.fromCompletableFuture(af.delay(session.executeAsync(p).toCompletableFuture)))
      .flatMap(page)
      .map(it => extractor(it, this))
  }

  def executeQuery[T](cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(info: ExecutionInfo, dc: Runner): Result[RunQueryResult[T]] =
    streamQuery[T](cql, prepare, extractor)(info, dc).compile.toList

  def executeQuerySingle[T](cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(info: ExecutionInfo, dc: Runner): Result[RunQuerySingleResult[T]] =
    Functor[F].map(executeQuery(cql, prepare, extractor)(info, dc))(handleSingleResult)

  def executeAction(cql: String, prepare: Prepare = identityPrepare)(info: ExecutionInfo, dc: Runner): Result[RunActionResult] = {
    prepareRowAndLog(cql, prepare)
      .flatMap(r => af.fromCompletableFuture(af.delay(session.executeAsync(r).toCompletableFuture)))
      .map(_ => ())
  }

  def executeBatchAction(groups: List[BatchGroup])(info: ExecutionInfo, dc: Runner): Result[RunBatchActionResult] =
    groups.traverse_ {
      case BatchGroup(cql, prepare) =>
        prepare.traverse_(executeAction(cql, _)(info, dc))
    }
}

object CassandraCeContext {

  def apply[N <: NamingStrategy, F[_]: Async: FlatMap](naming: N, config: CassandraContextConfig): CassandraCeContext[N, F] =
    new CassandraCeContext(naming, config.session, config.preparedStatementCacheSize)

  def apply[N <: NamingStrategy, F[_]: Async: FlatMap](naming: N, config: Config): CassandraCeContext[N, F] =
    CassandraCeContext(naming, CassandraContextConfig(config))

  def apply[N <: NamingStrategy, F[_]: Async: FlatMap](naming: N, configPrefix: String): CassandraCeContext[N, F] =
    CassandraCeContext(naming, LoadConfig(configPrefix))

}
