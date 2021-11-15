package io.getquill.context.qzio

import io.getquill.context.ZioJdbc._
import io.getquill.context.jdbc.JdbcComposition
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.context.{ ExecutionInfo, PrepareContext, StreamingContext, TranslateContextMacro }
import io.getquill.{ NamingStrategy, ReturnAction }
import zio.Exit.{ Failure, Success }
import zio.stream.ZStream
import zio.{ FiberRef, Has, Runtime, UIO, ZIO, ZManaged }

import java.sql.{ Array => _, _ }
import javax.sql.DataSource
import scala.util.Try

/**
 * Quill context that executes JDBC queries inside of ZIO. Unlike most other contexts
 * that require passing in a Data Source, this context takes in a java.sql.Connection
 * as a resource dependency which can be provided later (see `ZioJdbc` for helper methods
 * that assist in doing this).
 *
 * The resource dependency itself is just a `Has[Connection]`. Since this is frequently used
 * The type `QIO[T]` i.e. Quill-IO has been defined as an alias for `ZIO[Has[Connection], SQLException, T]`.
 *
 * Since in most JDBC use-cases, a connection-pool datasource i.e. Hikari is used it would actually
 * be much more useful to interact with `ZIO[Has[DataSource], SQLException, T]`.
 * The extension method `.onDataSource` in `io.getquill.context.ZioJdbc.QuillZioExt` will perform this conversion
 * (for even more brevity use `onDS` which is an alias for this method).
 * {{
 *   import ZioJdbc._
 *   val zioDs = DataSourceLayer.fromPrefix("testPostgresDB")
 *   MyZioContext.run(query[Person]).onDataSource.provideCustomLayer(zioDS)
 * }}
 *
 * If you are using a Plain Scala app however, you will need to manually run it e.g. using zio.Runtime
 * {{
 *   Runtime.default.unsafeRun(MyZioContext.run(query[Person]).provideLayer(zioDS))
 * }}
 *
 * Note however that the one exception to these cases are the `prepare` methods where a `ZIO[Has[Connection], SQLException, PreparedStatement]`
 * is being returned. In those situations the acquire-action-release pattern does not make any sense because the `PrepareStatement`
 * is only held open while it's host-connection exists.
 */
abstract class ZioJdbcContext[Dialect <: SqlIdiom, Naming <: NamingStrategy] extends ZioContext[Dialect, Naming]
  with JdbcComposition[Dialect, Naming]
  with StreamingContext[Dialect, Naming]
  with PrepareContext
  with TranslateContextMacro {

  override type StreamResult[T] = ZStream[Environment, Error, T]
  override type Result[T] = ZIO[Environment, Error, T]
  override type RunQueryResult[T] = List[T]
  override type RunQuerySingleResult[T] = T
  override type RunActionResult = Long
  override type RunActionReturningResult[T] = T
  override type RunBatchActionResult = List[Long]
  override type RunBatchActionReturningResult[T] = List[T]

  override type Error = SQLException
  override type Environment = Has[DataSource]
  override type PrepareRow = PreparedStatement
  override type ResultRow = ResultSet

  override type TranslateResult[T] = ZIO[Environment, Error, T]
  override type PrepareQueryResult = QCIO[PrepareRow]
  override type PrepareActionResult = QCIO[PrepareRow]
  override type PrepareBatchActionResult = QCIO[List[PrepareRow]]
  override type Session = Connection

  val currentConnection: FiberRef[Option[Connection]] =
    Runtime.default.unsafeRun(FiberRef.make(None))

  val underlying: ZioJdbcUnderlyingContext[Dialect, Naming]

  override def close() = ()

  override def probe(sql: String): Try[_] = underlying.probe(sql)

  def executeAction[T](sql: String, prepare: Prepare = identityPrepare)(info: ExecutionInfo, dc: DatasourceContext): QIO[Long] =
    onConnection(underlying.executeAction[T](sql, prepare)(info, dc))

  def executeQuery[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(info: ExecutionInfo, dc: DatasourceContext): QIO[List[T]] =
    onConnection(underlying.executeQuery[T](sql, prepare, extractor)(info, dc))

  override def executeQuerySingle[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(info: ExecutionInfo, dc: DatasourceContext): QIO[T] =
    onConnection(underlying.executeQuerySingle[T](sql, prepare, extractor)(info, dc))

  override def translateQuery[T](statement: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor, prettyPrint: Boolean = false)(executionInfo: ExecutionInfo, dc: DatasourceContext): TranslateResult[String] =
    onConnection(underlying.translateQuery[T](statement, prepare, extractor, prettyPrint)(executionInfo, dc))

  override def translateBatchQuery(groups: List[BatchGroup], prettyPrint: Boolean = false)(executionInfo: ExecutionInfo, dc: DatasourceContext): TranslateResult[List[String]] =
    onConnection(underlying.translateBatchQuery(groups.asInstanceOf[List[ZioJdbcContext.this.underlying.BatchGroup]], prettyPrint)(executionInfo, dc))

  def streamQuery[T](fetchSize: Option[Int], sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(info: ExecutionInfo, dc: DatasourceContext): QStream[T] =
    onConnectionStream(underlying.streamQuery[T](fetchSize, sql, prepare, extractor)(info, dc))

  def executeActionReturning[O](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[O], returningBehavior: ReturnAction)(info: ExecutionInfo, dc: DatasourceContext): QIO[O] =
    onConnection(underlying.executeActionReturning[O](sql, prepare, extractor, returningBehavior)(info, dc))

  def executeBatchAction(groups: List[BatchGroup])(info: ExecutionInfo, dc: DatasourceContext): QIO[List[Long]] =
    onConnection(underlying.executeBatchAction(groups.asInstanceOf[List[ZioJdbcContext.this.underlying.BatchGroup]])(info, dc))

  def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: Extractor[T])(info: ExecutionInfo, dc: DatasourceContext): QIO[List[T]] =
    onConnection(underlying.executeBatchActionReturning[T](groups.asInstanceOf[List[ZioJdbcContext.this.underlying.BatchGroupReturning]], extractor)(info, dc))

  def prepareQuery(sql: String, prepare: Prepare)(info: ExecutionInfo, dc: DatasourceContext): QCIO[PreparedStatement] =
    underlying.prepareQuery(sql, prepare)(info, dc)

  def prepareAction(sql: String, prepare: Prepare)(info: ExecutionInfo, dc: DatasourceContext): QCIO[PreparedStatement] =
    underlying.prepareAction(sql, prepare)(info, dc)

  def prepareBatchAction(groups: List[BatchGroup])(info: ExecutionInfo, dc: DatasourceContext): QCIO[List[PreparedStatement]] =
    underlying.prepareBatchAction(groups.asInstanceOf[List[ZioJdbcContext.this.underlying.BatchGroup]])(info, dc)

  private[getquill] def prepareParams(statement: String, prepare: Prepare): QCIO[Seq[String]] =
    underlying.prepareParams(statement, prepare)

  /**
   * Execute instructions in a transaction. For example, to add a Person row to the database and return
   * the contents of the Person table immediately after that:
   * {{{
   *   val a = run(query[Person].insert(Person(...)): ZIO[Has[DataSource], SQLException, Long]
   *   val b = run(query[Person]): ZIO[Has[DataSource], SQLException, Person]
   *   transaction(a *> b): ZIO[Has[DataSource], SQLException, Person]
   * }}}
   *
   * The order of operations run in the case that a new connection needs to be aquired are as follows:
   * <pre>
   *   getDS from env,
   *   acquire-connection,
   *     set-no-autocommit(connection),
   *       put-into-fiberref(connection),
   *         op - the corresponding execute_ method which will execute and pull connection from the fiberref,
   *       remove-from-fiberref(connection),
   *     set-prev-autocommit(connection),
   *   release-conn
   * </pre>
   */
  def transaction[R <: Has[DataSource], A](op: ZIO[R, Throwable, A]): ZIO[R, Throwable, A] = {
    withBlocking(currentConnection.get.flatMap {
      // We can just return the op in the case that there is already a connection set on the fiber ref
      // because the op is execute___ which will lookup the connection from the fiber ref via onConnection/onConnectionStream
      // This will typically happen for nested transactions e.g. transaction(transaction(a *> b) *> c)
      case Some(connection) => op
      case None =>
        val connection = for {
          env <- ZIO.service[DataSource].toManaged_
          connection <- managedBestEffort(blockingEffect(env.getConnection))
          // Get the current value of auto-commit
          prevAutoCommit <- blockingEffect(connection.getAutoCommit).toManaged_
          // Disable auto-commit since we need to be able to roll back. Once everything is done, set it
          // to whatever the previous value was.
          _ <- ZManaged.make(blockingEffect(connection.setAutoCommit(false))) { _ =>
            blockingEffect(connection.setAutoCommit(prevAutoCommit)).orDie
          }
          _ <- ZManaged.make(currentConnection.set(Some(connection))) { _ =>
            // Note. We are failing the fiber if auto-commit reset fails. For some circumstances this may be too aggresive.
            // If the connection pool e.g. Hikari resets this property for a recycled connection anyway doing it here
            // might not be necessary
            currentConnection.set(None)
          }
          // Once the `use` of this outer-ZManaged is done, rollback the connection if needed
          _ <- ZManaged.finalizerExit {
            case Success(_)     => withBlocking(UIO(connection.commit()))
            case Failure(cause) => withBlocking(UIO(connection.rollback()))
          }
        } yield ()

        connection.use_(op)
    })
  }

  private def onConnection[T](qlio: ZIO[Has[Connection], SQLException, T]): ZIO[Has[DataSource], SQLException, T] =
    currentConnection.get.flatMap {
      case Some(connection) =>
        withBlocking(qlio.provide(Has(connection)))
      case None =>
        withBlocking(qlio.provideLayer(DataSourceLayer.live))
    }

  private def onConnectionStream[T](qstream: ZStream[Has[Connection], SQLException, T]): ZStream[Has[DataSource], SQLException, T] =
    streamBlocker *> ZStream.fromEffect(currentConnection.get).flatMap {
      case Some(connection) =>
        qstream.provide(Has(connection))
      case None =>
        qstream.provideLayer(DataSourceLayer.live).refineToOrDie[SQLException]
    }
}