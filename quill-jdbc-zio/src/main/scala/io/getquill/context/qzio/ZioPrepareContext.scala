package io.getquill.context.qzio

import io.getquill.NamingStrategy
import io.getquill.context.PrepareContext
import io.getquill.context.ZioJdbc._
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.util.ContextLogger
import zio.blocking.Blocking
import zio.{ Has, ZIO }

import java.sql.{ Connection, PreparedStatement, ResultSet, SQLException }

trait ZioPrepareContext[Dialect <: SqlIdiom, Naming <: NamingStrategy] extends ZioContext[Dialect, Naming]
  with PrepareContext {

  private[getquill] val logger = ContextLogger(classOf[ZioPrepareContext[_, _]])

  override type PrepareRow = PreparedStatement
  override type ResultRow = ResultSet
  override type PrepareQueryResult = QIO[PrepareRow]
  override type PrepareActionResult = QIO[PrepareRow]
  override type PrepareBatchActionResult = QIO[List[PrepareRow]]

  def prepareQuery[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): PrepareQueryResult =
    prepareSingle(sql, prepare)

  def prepareAction(sql: String, prepare: Prepare = identityPrepare): PrepareActionResult =
    prepareSingle(sql, prepare)

  /** Execute SQL on connection and return prepared statement. Closes the statement in a bracket. */
  def prepareSingle(sql: String, prepare: Prepare = identityPrepare): QIO[PreparedStatement] = {
    ZIO.environment[QConnection]
      .mapEffect(bconn => bconn.get[Connection].prepareStatement(sql))
      .mapEffect { stmt =>
        val (params, ps) = prepare(stmt)
        logger.logQuery(sql, params)
        ps
      }.refineToOrDie[SQLException]
  }

  def prepareBatchAction(groups: List[BatchGroup]): PrepareBatchActionResult =
    ZIO.collectAll[Has[Connection] with Blocking, Throwable, PrepareRow, List] {
      val batches = groups.flatMap {
        case BatchGroup(sql, prepares) =>
          prepares.map(sql -> _)
      }
      batches.map {
        case (sql, prepare) =>
          prepareSingle(sql, prepare)
      }
    }.refineToOrDie[SQLException]
}
