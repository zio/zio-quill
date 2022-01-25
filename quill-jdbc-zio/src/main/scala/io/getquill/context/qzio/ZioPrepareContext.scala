package io.getquill.context.qzio

import io.getquill.NamingStrategy
import io.getquill.context.{ ExecutionInfo, PrepareContext }
import io.getquill.context.ZioJdbc._
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.util.ContextLogger
import zio.{ Has, Task, ZIO }

import java.sql.{ Connection, PreparedStatement, ResultSet, SQLException }

trait ZioPrepareContext[Dialect <: SqlIdiom, Naming <: NamingStrategy] extends ZioContext[Dialect, Naming]
  with PrepareContext {

  private[getquill] val logger = ContextLogger(classOf[ZioPrepareContext[_, _]])

  override type PrepareRow = PreparedStatement
  override type ResultRow = ResultSet
  override type PrepareQueryResult = QCIO[PrepareRow]
  override type PrepareActionResult = QCIO[PrepareRow]
  override type PrepareBatchActionResult = QCIO[List[PrepareRow]]
  override type Session = Connection

  def prepareQuery(sql: String, prepare: Prepare = identityPrepare)(info: ExecutionInfo, dc: Runner): PrepareQueryResult =
    prepareSingle(sql, prepare)(info, dc)

  def prepareAction(sql: String, prepare: Prepare = identityPrepare)(info: ExecutionInfo, dc: Runner): PrepareActionResult =
    prepareSingle(sql, prepare)(info, dc)

  /** Execute SQL on connection and return prepared statement. Closes the statement in a bracket. */
  def prepareSingle(sql: String, prepare: Prepare = identityPrepare)(info: ExecutionInfo, dc: Runner): QCIO[PreparedStatement] = {
    (for {
      bconn <- ZIO.environment[Has[Connection]]
      conn = bconn.get[Connection]
      stmt <- Task(conn.prepareStatement(sql))
      ps <- Task {
        val (params, ps) = prepare(stmt, conn)
        logger.logQuery(sql, params)
        ps
      }
    } yield ps).refineToOrDie[SQLException]
  }

  def prepareBatchAction(groups: List[BatchGroup])(info: ExecutionInfo, dc: Runner): PrepareBatchActionResult =
    ZIO.collectAll[Has[Connection], Throwable, PrepareRow, List] {
      val batches = groups.flatMap {
        case BatchGroup(sql, prepares) =>
          prepares.map(sql -> _)
      }
      batches.map {
        case (sql, prepare) =>
          prepareSingle(sql, prepare)(info, dc)
      }
    }.refineToOrDie[SQLException]
}
