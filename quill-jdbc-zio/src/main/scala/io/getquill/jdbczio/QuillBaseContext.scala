package io.getquill.jdbczio

import io.getquill.ast.ScalarLift
import io.getquill.{NamingStrategy, ReturnAction}
import io.getquill.context.{ContextVerbStream, ExecutionInfo, ProtoContext, TranslateOptions}
import io.getquill.context.jdbc.JdbcContextTypes
import io.getquill.context.qzio.{ZioContext, ZioJdbcContext, ZioTranslateContext}
import io.getquill.context.sql.idiom.SqlIdiom
import zio.{ZEnvironment, ZIO}
import zio.stream.ZStream

import java.sql.{Connection, PreparedStatement, ResultSet, SQLException}
import javax.sql.DataSource
import scala.util.Try

trait QuillBaseContext[+Dialect <: SqlIdiom, +Naming <: NamingStrategy]
    extends ZioContext[Dialect, Naming]
    with JdbcContextTypes[Dialect, Naming]
    with ProtoContext[Dialect, Naming]
    with ContextVerbStream[Dialect, Naming]
    with ZioTranslateContext {

  def ds: DataSource

  override type StreamResult[T]                  = ZStream[Environment, Error, T]
  override type Result[T]                        = ZIO[Environment, Error, T]
  override type RunQueryResult[T]                = List[T]
  override type RunQuerySingleResult[T]          = T
  override type RunActionResult                  = Long
  override type RunActionReturningResult[T]      = T
  override type RunBatchActionResult             = List[Long]
  override type RunBatchActionReturningResult[T] = List[T]

  override type Error       = SQLException
  override type Environment = Any
  override type PrepareRow  = PreparedStatement
  override type ResultRow   = ResultSet

  override type TranslateResult[T] = ZIO[Environment, Error, T]
  override type Session            = Connection

  lazy val underlying: ZioJdbcContext[Dialect, Naming] = dsDelegate
  private[getquill] val dsDelegate: ZioJdbcContext[Dialect, Naming]

  override def close() = ()

  override def probe(sql: String): Try[_] = dsDelegate.probe(sql)

  def executeAction(sql: String, prepare: Prepare = identityPrepare)(
    info: ExecutionInfo,
    dc: Runner
  ): ZIO[Any, SQLException, Long] =
    onDS(dsDelegate.executeAction(sql, prepare)(info, dc))

  def executeQuery[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(
    info: ExecutionInfo,
    dc: Runner
  ): ZIO[Any, SQLException, List[T]] =
    onDS(dsDelegate.executeQuery[T](sql, prepare, extractor)(info, dc))

  override def executeQuerySingle[T](
    sql: String,
    prepare: Prepare = identityPrepare,
    extractor: Extractor[T] = identityExtractor
  )(info: ExecutionInfo, dc: Runner): ZIO[Any, SQLException, T] =
    onDS(dsDelegate.executeQuerySingle[T](sql, prepare, extractor)(info, dc))

  override def translateQuery[T](
    statement: String,
    lifts: List[ScalarLift] = List(),
    options: TranslateOptions
  )(executionInfo: ExecutionInfo, dc: Runner): String =
    dsDelegate.translateQuery[T](statement, lifts, options)(executionInfo, dc)

  override def translateBatchQuery(
    groups: List[BatchGroup],
    options: TranslateOptions
  )(executionInfo: ExecutionInfo, dc: Runner): List[String] =
    dsDelegate.translateBatchQuery(
      groups.asInstanceOf[List[QuillBaseContext.this.dsDelegate.BatchGroup]],
      options
    )(executionInfo, dc)

  def streamQuery[T](
    fetchSize: Option[Int],
    sql: String,
    prepare: Prepare = identityPrepare,
    extractor: Extractor[T] = identityExtractor
  )(info: ExecutionInfo, dc: Runner): ZStream[Any, SQLException, T] =
    onDSStream(dsDelegate.streamQuery[T](fetchSize, sql, prepare, extractor)(info, dc))

  def executeActionReturning[O](
    sql: String,
    prepare: Prepare = identityPrepare,
    extractor: Extractor[O],
    returningBehavior: ReturnAction
  )(info: ExecutionInfo, dc: Runner): ZIO[Any, SQLException, O] =
    onDS(dsDelegate.executeActionReturning[O](sql, prepare, extractor, returningBehavior)(info, dc))

  def executeActionReturningMany[O](
    sql: String,
    prepare: Prepare = identityPrepare,
    extractor: Extractor[O],
    returningBehavior: ReturnAction
  )(info: ExecutionInfo, dc: Runner): ZIO[Any, SQLException, List[O]] =
    onDS(dsDelegate.executeActionReturningMany[O](sql, prepare, extractor, returningBehavior)(info, dc))

  def executeBatchAction(
    groups: List[BatchGroup]
  )(info: ExecutionInfo, dc: Runner): ZIO[Any, SQLException, List[Long]] =
    onDS(
      dsDelegate.executeBatchAction(groups.asInstanceOf[List[QuillBaseContext.this.dsDelegate.BatchGroup]])(info, dc)
    )

  def executeBatchActionReturning[T](
    groups: List[BatchGroupReturning],
    extractor: Extractor[T]
  )(info: ExecutionInfo, dc: Runner): ZIO[Any, SQLException, List[T]] =
    onDS(
      dsDelegate.executeBatchActionReturning[T](
        groups.asInstanceOf[List[QuillBaseContext.this.dsDelegate.BatchGroupReturning]],
        extractor
      )(info, dc)
    )

  // Used in translation functions
  private[getquill] def prepareParams(statement: String, prepare: Prepare): ZIO[Any, SQLException, Seq[String]] =
    onDS(dsDelegate.prepareParams(statement, prepare))

  /**
   * Execute instructions in a transaction. For example, to add a Person row to
   * the database and return the contents of the Person table immediately after
   * that:
   * {{{
   *   val a = run(query[Person].insert(Person(...)): ZIO[DataSource, SQLException, Long]
   *   val b = run(query[Person]): ZIO[DataSource, SQLException, Person]
   *   transaction(a *> b): ZIO[DataSource, SQLException, Person]
   * }}}
   */
  def transaction[R, A](op: ZIO[R, Throwable, A]): ZIO[R, Throwable, A] =
    dsDelegate.transaction(op).provideSomeEnvironment[R]((env: ZEnvironment[R]) => env.add[DataSource](ds: DataSource))

  private def onDS[T](qio: ZIO[DataSource, SQLException, T]): ZIO[Any, SQLException, T] =
    qio.provideEnvironment(ZEnvironment(ds))

  private def onDSStream[T](qstream: ZStream[DataSource, SQLException, T]): ZStream[Any, SQLException, T] =
    qstream.provideEnvironment(ZEnvironment(ds))
}
