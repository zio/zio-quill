package io.getquill.context.zio

import io.getquill.NamingStrategy
import io.getquill.context.PrepareContext
import io.getquill.context.ZioJdbc._
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.util.ContextLogger
import zio.blocking.Blocking
import zio.{ Has, RIO, ZIO }

import java.sql.{ Connection, PreparedStatement, ResultSet }

trait ZioPrepareContext[Dialect <: SqlIdiom, Naming <: NamingStrategy] extends ZioContext[Dialect, Naming]
  with PrepareContext {

  private[getquill] val logger = ContextLogger(classOf[ZioPrepareContext[_, _]])

  override type PrepareRow = PreparedStatement
  override type ResultRow = ResultSet
  override type PrepareQueryResult = RIO[BlockingConnection, PrepareRow]
  override type PrepareActionResult = RIO[BlockingConnection, PrepareRow]
  override type PrepareBatchActionResult = RIO[BlockingConnection, List[PrepareRow]]

  def prepareQuery[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): PrepareQueryResult =
    prepareSingle(sql, prepare)

  def prepareAction(sql: String, prepare: Prepare = identityPrepare): PrepareActionResult =
    prepareSingle(sql, prepare)

  /** Execute SQL on connection and return prepared statement. Closes the statement in a bracket. */
  def prepareSingle(sql: String, prepare: Prepare = identityPrepare): RIO[BlockingConnection, PreparedStatement] = {
    ZIO.environment[BlockingConnection]
      .mapEffect(bconn => bconn.get[Connection].prepareStatement(sql))
      .mapEffect { stmt =>
        val (params, ps) = prepare(stmt)
        logger.logQuery(sql, params)
        ps
      }
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
    }
}
