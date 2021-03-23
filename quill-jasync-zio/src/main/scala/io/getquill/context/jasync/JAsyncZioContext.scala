package io.getquill.context.jasync

import com.github.jasync.sql.db.pool.ConnectionPool
import com.github.jasync.sql.db.{ Connection, QueryResult, RowData }
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.util.ContextLogger
import io.getquill.{ NamingStrategy, ReturnAction }
import izumi.reflect.Tag
import kotlin.jvm.functions.Function1
import zio.{ Has, Task, ZIO }

import java.util.concurrent.CompletableFuture
import scala.jdk.CollectionConverters._
import scala.util.Try

object JAsyncZioContext {
  implicit class CompleteableFutureOps[T](cf: CompletableFuture[T]) {
    def toZio: Task[T] = ZIO.fromCompletionStage(cf)
  }
}

abstract class JAsyncZioContext[D <: SqlIdiom, N <: NamingStrategy, C <: Connection: Tag](val idiom: D, val naming: N)
  extends JAsyncContextBase[D, N] {

  import JAsyncZioContext._

  private val logger = ContextLogger(classOf[JAsyncZioContext[_, _, _]])

  override type PrepareRow = Seq[Any]
  override type ResultRow = RowData

  override type Result[T] = ZIO[Has[C], Throwable, T]
  override type RunQueryResult[T] = Seq[T]
  override type RunQuerySingleResult[T] = T
  override type RunActionResult = Long
  override type RunActionReturningResult[T] = T
  override type RunBatchActionResult = Seq[Long]
  override type RunBatchActionReturningResult[T] = Seq[T]

  //  implicit def toCompletableFuture[T](f: Future[T]): CompletableFuture[T] = FutureConverters.toJava(f).asInstanceOf[CompletableFuture[T]]

  def toKotlin[T, R](f: T => R): Function1[T, R] = new Function1[T, R] {
    override def invoke(t: T): R = f(t)
  }

  override def close = throw new IllegalArgumentException("This operation is not relevant in ZIO contexts since they are stateless.")
  protected def extractActionResult[O](returningAction: ReturnAction, extractor: Extractor[O])(result: QueryResult): O
  protected def expandAction(sql: String, returningAction: ReturnAction) = sql

  def probingDataSource: Option[ConnectionPool[_]] = None

  def probe(sql: String) =
    probingDataSource match {
      case None =>
        scala.util.Success(())
      case Some(pool) =>
        Try {
          zio.Runtime.default.unsafeRun(pool.sendQuery(sql).toZio)
        }
    }

  def transaction[T](ops: ZIO[Has[C], Throwable, T]) = {
    for {
      env <- ZIO.environment[Has[C]]
      conn = env.get[C]
      rt <- ZIO.runtime[Any]
      result <- conn.inTransaction(toKotlin(
        (conn: Connection) => rt.unsafeRun(ops.provide(Has(conn.asInstanceOf[C])).toCompletableFuture)
      )).toZio
    } yield result
  }

  def prepareAndSend(sql: String, conn: C, prepare: Prepare) = {
    val (params, values) = prepare(Nil)
    logger.logQuery(sql, params)
    conn.sendPreparedStatement(sql, values.asJava)
  }

  def executeQuery[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): ZIO[Has[C], Throwable, List[T]] = {
    for {
      env <- ZIO.environment[Has[C]]
      conn = env.get[C]
      rows <- ZIO.succeed(prepareAndSend(sql, conn, prepare).toZio).flatten
      results <- ZIO.effect(rows.getRows.asScala.iterator.map(extractor).toList)
    } yield results
  }

  def executeQuerySingle[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): ZIO[Has[C], Throwable, T] =
    executeQuery(sql, prepare, extractor).map(handleSingleResult)

  def executeAction[T](sql: String, prepare: Prepare = identityPrepare): ZIO[Has[C], Throwable, Long] = {
    for {
      env <- ZIO.environment[Has[C]]
      conn = env.get[C]
      rows <- ZIO.succeed(prepareAndSend(sql, conn, prepare).toZio).flatten
      result <- ZIO.effect(rows.getRowsAffected)
    } yield result
  }

  def executeActionReturning[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T], returningAction: ReturnAction): ZIO[Has[C], Throwable, T] = {
    for {
      env <- ZIO.environment[Has[C]]
      conn = env.get[C]
      rows <- ZIO.succeed(prepareAndSend(sql, conn, prepare).toZio).flatten
      result <- ZIO.effect(extractActionResult(returningAction, extractor)(rows))
    } yield result
  }

  def executeBatchAction(groups: List[BatchGroup]): ZIO[Has[C], Throwable, List[Long]] = {
    def prep(conn: C) =
      for {
        BatchGroup(sql, prepares) <- groups
        prepare <- prepares
      } yield executeAction(sql, prepare).provide(Has(conn)) // TODO is it necessary to do a provide conn here or can I reply on that coming from the upper-level function?

    // TODO not sure why the JAsync version uses foldLeft and list builder. I think it's the same perf as a full list
    for {
      env <- ZIO.environment[Has[C]]
      conn = env.get[C]
      jobs <- ZIO.effect(prep(conn))
      results <- ZIO.collectAll(jobs)
    } yield results
  }

  def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: Extractor[T]): ZIO[Has[C], Throwable, List[T]] = {
    def prep(conn: C) =
      for {
        BatchGroupReturning(sql, column, prepares) <- groups
        prepare <- prepares
      } yield executeActionReturning(sql, prepare, extractor, column).provide(Has(conn)) // TODO is it necessary to do a provide conn here or can I reply on that coming from the upper-level function?

    // TODO not sure why the JAsync version uses foldLeft and list builder. I think it's the same perf as a full list
    for {
      env <- ZIO.environment[Has[C]]
      conn = env.get[C]
      jobs <- ZIO.effect(prep(conn))
      results <- ZIO.collectAll(jobs)
    } yield results
  }

  override private[getquill] def prepareParams(statement: String, prepare: Prepare): Seq[String] =
    prepare(Nil)._2.map(prepareParam)

}