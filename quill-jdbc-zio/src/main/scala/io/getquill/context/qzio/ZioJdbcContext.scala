package io.getquill.context.qzio

import io.getquill.context.ZioJdbc._
import io.getquill.context.jdbc.JdbcComposition
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.context.{ ExecutionInfo, PrepareContext, StreamingContext, TranslateContextMacro }
import io.getquill.{ NamingStrategy, ReturnAction }
import zio.Exit.{ Failure, Success }
import zio.stream.ZStream
import zio.{ Cause, FiberRef, Has, Runtime, UIO, ZIO, ZManaged }

import java.io.Closeable
import java.sql.{ Array => _, _ }
import javax.sql.DataSource
import scala.reflect.ClassTag
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
 * be much more useful to interact with `ZIO[Has[DataSource with Closeable], SQLException, T]`.
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
  override type Environment = Has[DataSource with Closeable]
  override type PrepareRow = PreparedStatement
  override type ResultRow = ResultSet

  override type TranslateResult[T] = ZIO[Environment, Error, T]
  override type PrepareQueryResult = QIO[PrepareRow]
  override type PrepareActionResult = QIO[PrepareRow]
  override type PrepareBatchActionResult = QIO[List[PrepareRow]]
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

  def prepareQuery(sql: String, prepare: Prepare)(info: ExecutionInfo, dc: DatasourceContext): QIO[PreparedStatement] =
    onConnection(underlying.prepareQuery(sql, prepare)(info, dc))

  def prepareAction(sql: String, prepare: Prepare)(info: ExecutionInfo, dc: DatasourceContext): QIO[PreparedStatement] =
    onConnection(underlying.prepareAction(sql, prepare)(info, dc))

  def prepareBatchAction(groups: List[BatchGroup])(info: ExecutionInfo, dc: DatasourceContext): QIO[List[PreparedStatement]] =
    onConnection(underlying.prepareBatchAction(groups.asInstanceOf[List[ZioJdbcContext.this.underlying.BatchGroup]])(info, dc))

  /**
   * Execute instructions in a transaction. For example, to add a Person row to the database and return
   * the contents of the Person table immediately after that:
   * {{
   *   val a = run(query[Person].insert(Person(...)): ZIO[Has[DataSource with Closable], SQLException, Long]
   *   val b = run(query[Person]): ZIO[Has[DataSource with Closable], SQLException, Person]
   *   transaction(a *> b): ZIO[Has[DataSource with Closable], SQLException, Person]
   * }}
   */
  def transaction[R <: Has[DataSource with Closeable], A](op: ZIO[R, Throwable, A]): ZIO[R, Throwable, A] =
    currentConnection.get.flatMap {
      // We can just return the op in the case that there is already a connection set on the fiber ref
      // because the op is execute___ which will lookup the connection from the fiber ref via onConnection/onConnectionStream
      // This will typically happen for nested transactions e.g. transaction(transaction(a *> b) *> c)
      // TODO this needs to be simpleBlocking
      case Some(connection) => op
      case None =>
        val connection = for {
          env <- ZIO.service[DataSource with Closeable].toManaged_
          connection <- managedBestEffort(ZIO.effect(env.getConnection))
          prevAutoCommit <- ZIO.effect(connection.getAutoCommit).toManaged_
          _ <- ZIO.effect(connection.setAutoCommit(false)).toManaged_
          _ <- ZManaged.make(currentConnection.set(Some(connection)))(_ =>
            // Note. Do we really need to fail the fiber if the auto-commit reset does not happen?
            // Would hikari do this for us anyway
            currentConnection.set(None) *> ZIO.effect(connection.setAutoCommit(prevAutoCommit)).orDie)
          // TODO Ask Adam about this, this always has to happen when the currentConnection variable is unset
          //      (usually right before the connection is returned to the pool). Why wouldn't it work if below?
          //_ <- ZIO.effect(connection.setAutoCommit(prevAutoCommit)).toManaged_
          _ <- op.onExit {
            case Success(_) =>
              UIO(connection.commit())
            case Failure(cause) =>
              UIO(connection.rollback()).foldCauseM(
                // NOTE: cause.flatMap(Cause.die) means wrap up the throwable failures into die failures, can only do if E param is Throwable (can also do .orDie at the end)
                rollbackFailCause => ZIO.halt(cause.flatMap(Cause.die) ++ rollbackFailCause),
                _ => ZIO.halt(cause.flatMap(Cause.die)) // or ZIO.halt(cause).orDie
              )
          }.toManaged_
        } yield ()
        connection.use_(op)
    }

  private def onConnection[T](qlio: ZIO[Has[Connection], SQLException, T]): ZIO[Has[DataSource with Closeable], SQLException, T] =
    // TODO this needs to be simpleBlocking
    currentConnection.get.flatMap {
      case Some(connection) =>
        qlio.provide(Has(connection))
      case None =>
        qlio.onDataSource
    }

  private def onConnectionStream[T](qstream: ZStream[Has[Connection], SQLException, T]): ZStream[Has[DataSource with Closeable], SQLException, T] =
    // TODO this needs to use the blocking stream runner
    ZStream.fromEffect(currentConnection.get).flatMap {
      case Some(connection) =>
        qstream.provide(Has(connection))
      case None =>
        qstream.provideLayer(DataSourceLayer.live).refineToOrDie[SQLException]
    }
}