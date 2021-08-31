package io.getquill

import cats._
import cats.effect._
import com.datastax.driver.core._
import cats.syntax.all._
import fs2.Stream
import com.typesafe.config.Config
import io.getquill.context.cassandra.CqlIdiom
import io.getquill.util.{ ContextLogger, LoadConfig }
import io.getquill.util.GuavaCeUtils._
import io.getquill.context.ExecutionInfo
import io.getquill.context.ce.CeContext

import scala.jdk.CollectionConverters._
import scala.language.higherKinds

class CassandraCeContext[N <: NamingStrategy, F[_]](
  naming:                     N,
  cluster:                    Cluster,
  keyspace:                   String,
  preparedStatementCacheSize: Long
)(implicit val af: Async[F])
  extends CassandraClusterSessionContext[N](naming, cluster, keyspace, preparedStatementCacheSize)
  with CeContext[CqlIdiom, N, F] {

  private val logger = ContextLogger(classOf[CassandraCeContext[_, F]])

  private[getquill] def prepareRowAndLog(cql: String, prepare: Prepare = identityPrepare): F[PrepareRow] = for {
    ec <- Async[F].executionContext
    futureStatement = Sync[F].delay(prepareAsync(cql)(ec))
    prepStatement <- Async[F].fromFuture(futureStatement)
    (params, bs) = prepare(prepStatement, this)
    _ <- Sync[F].delay(logger.logQuery(cql, params))
  } yield bs

  protected def page(rs: ResultSet): F[Iterable[Row]] = for {
    available <- af.delay(rs.getAvailableWithoutFetching)
    page_isFullyFetched <- af.delay {
      (rs.asScala.take(available), rs.isFullyFetched)
    }
    (page, isFullyFetched) = page_isFullyFetched
    it <- if (isFullyFetched)
      af.delay {
        page
      }
    else {
      af.delay {
        rs.fetchMoreResults()
      }.toAsync.map(_ => page)
    }
  } yield it

  def streamQuery[T](cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(info: ExecutionInfo, dc: DatasourceContext): StreamResult[T] = {
    Stream
      .eval(prepareRowAndLog(cql, prepare))
      .evalMap(p => af.delay(session.executeAsync(p)).toAsync)
      .flatMap(rs => Stream.repeatEval(page(rs)))
      .takeWhile(_.nonEmpty)
      .flatMap(Stream.iterable)
      .map(it => extractor(it, this))
  }

  def executeQuery[T](cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(info: ExecutionInfo, dc: DatasourceContext): Result[RunQueryResult[T]] =
    streamQuery[T](cql, prepare, extractor)(info, dc).compile.toList

  def executeQuerySingle[T](cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(info: ExecutionInfo, dc: DatasourceContext): Result[RunQuerySingleResult[T]] =
    Functor[F].map(executeQuery(cql, prepare, extractor)(info, dc))(handleSingleResult)

  def executeAction(cql: String, prepare: Prepare = identityPrepare)(info: ExecutionInfo, dc: DatasourceContext): Result[RunActionResult] = {
    prepareRowAndLog(cql, prepare)
      .flatMap(r => af.delay(session.executeAsync(r)).toAsync)
      .map(_ => ())
  }

  def executeBatchAction(groups: List[BatchGroup])(info: ExecutionInfo, dc: DatasourceContext): Result[RunBatchActionResult] =
    groups.traverse_ {
      case BatchGroup(cql, prepare) =>
        prepare.traverse_(executeAction(cql, _)(info, dc))
    }
}

object CassandraCeContext {

  def apply[N <: NamingStrategy, F[_]: Async: FlatMap](naming: N, config: CassandraContextConfig): CassandraCeContext[N, F] =
    new CassandraCeContext(naming, config.cluster, config.keyspace, config.preparedStatementCacheSize)

  def apply[N <: NamingStrategy, F[_]: Async: FlatMap](naming: N, config: Config): CassandraCeContext[N, F] =
    CassandraCeContext(naming, CassandraContextConfig(config))

  def apply[N <: NamingStrategy, F[_]: Async: FlatMap](naming: N, configPrefix: String): CassandraCeContext[N, F] =
    CassandraCeContext(naming, LoadConfig(configPrefix))

}
