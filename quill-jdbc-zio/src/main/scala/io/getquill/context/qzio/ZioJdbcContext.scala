package io.getquill.context.qzio

import io.getquill.context.ZioJdbc._
import io.getquill.context.jdbc.JdbcComposition
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.context.{ ExecutionInfo, PrepareContext, StreamingContext, TranslateContextMacro }
import io.getquill.{ NamingStrategy, ReturnAction }
import zio.ZIO.ZIOAutoCloseableOps
import zio.stream.ZStream
import zio.{ Has, ZIO }

import java.io.Closeable
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

  val underlying: ZioJdbcUnderlyingContext[Dialect, Naming]

  override def close() = ()

  override def probe(sql: String): Try[_] = underlying.probe(sql)

  def executeAction[T](sql: String, prepare: Prepare = identityPrepare)(info: ExecutionInfo, dc: DatasourceContext): QIO[Long] =
    underlying.executeAction[T](sql, prepare)(info, dc).onDataSource

  def executeQuery[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(info: ExecutionInfo, dc: DatasourceContext): QIO[List[T]] =
    underlying.executeQuery[T](sql, prepare, extractor)(info, dc).onDataSource

  override def executeQuerySingle[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(info: ExecutionInfo, dc: DatasourceContext): QIO[T] =
    underlying.executeQuerySingle[T](sql, prepare, extractor)(info, dc).onDataSource

  override def translateQuery[T](statement: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor, prettyPrint: Boolean = false)(executionInfo: ExecutionInfo, dc: DatasourceContext): TranslateResult[String] =
    underlying.translateQuery[T](statement, prepare, extractor, prettyPrint)(executionInfo, dc).onDataSource

  override def translateBatchQuery(groups: List[BatchGroup], prettyPrint: Boolean = false)(executionInfo: ExecutionInfo, dc: DatasourceContext): TranslateResult[List[String]] =
    underlying.translateBatchQuery(groups.asInstanceOf[List[ZioJdbcContext.this.underlying.BatchGroup]], prettyPrint)(executionInfo, dc).onDataSource

  def streamQuery[T](fetchSize: Option[Int], sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(info: ExecutionInfo, dc: DatasourceContext): QStream[T] =
    underlying.streamQuery[T](fetchSize, sql, prepare, extractor)(info, dc).provideLayer(DataSourceLayer.live).refineToOrDie[SQLException]

  def executeActionReturning[O](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[O], returningBehavior: ReturnAction)(info: ExecutionInfo, dc: DatasourceContext): QIO[O] =
    underlying.executeActionReturning[O](sql, prepare, extractor, returningBehavior)(info, dc).onDataSource

  def executeBatchAction(groups: List[BatchGroup])(info: ExecutionInfo, dc: DatasourceContext): QIO[List[Long]] =
    underlying.executeBatchAction(groups.asInstanceOf[List[ZioJdbcContext.this.underlying.BatchGroup]])(info, dc).onDataSource

  def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: Extractor[T])(info: ExecutionInfo, dc: DatasourceContext): QIO[List[T]] =
    underlying.executeBatchActionReturning[T](groups.asInstanceOf[List[ZioJdbcContext.this.underlying.BatchGroupReturning]], extractor)(info, dc).onDataSource

  def prepareQuery(sql: String, prepare: Prepare)(info: ExecutionInfo, dc: DatasourceContext): QIO[PreparedStatement] =
    underlying.prepareQuery(sql, prepare)(info, dc).onDataSource

  def prepareAction(sql: String, prepare: Prepare)(info: ExecutionInfo, dc: DatasourceContext): QIO[PreparedStatement] =
    underlying.prepareAction(sql, prepare)(info, dc).onDataSource

  def prepareBatchAction(groups: List[BatchGroup])(info: ExecutionInfo, dc: DatasourceContext): QIO[List[PreparedStatement]] =
    underlying.prepareBatchAction(groups.asInstanceOf[List[ZioJdbcContext.this.underlying.BatchGroup]])(info, dc).onDataSource
}