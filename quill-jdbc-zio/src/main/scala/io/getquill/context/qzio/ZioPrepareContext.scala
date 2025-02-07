package io.getquill.context.qzio

import io.getquill.NamingStrategy
import io.getquill.context.{ExecutionInfo, ContextVerbPrepare}
import io.getquill.context.ZioJdbc._
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.util.ContextLogger
import zio.ZIO

import java.sql.{Connection, PreparedStatement, ResultSet, SQLException}

trait ZioPrepareContext[+Dialect <: SqlIdiom, +Naming <: NamingStrategy]
    extends ZioContext[Dialect, Naming]
    with ContextVerbPrepare {

  private[getquill] val logger = ContextLogger(classOf[ZioPrepareContext[_, _]])

  override type PrepareRow               = PreparedStatement
  override type ResultRow                = ResultSet
  override type PrepareQueryResult       = QCIO[PrepareRow]
  override type PrepareActionResult      = QCIO[PrepareRow]
  override type PrepareBatchActionResult = QCIO[List[PrepareRow]]
  override type Session                  = Connection

  def prepareQuery(sql: String, prepare: Prepare = identityPrepare)(
    info: ExecutionInfo,
    dc: Runner
  ): PrepareQueryResult =
    prepareSingle(sql, prepare)(info, dc)

  def prepareAction(sql: String, prepare: Prepare = identityPrepare)(
    info: ExecutionInfo,
    dc: Runner
  ): PrepareActionResult =
    prepareSingle(sql, prepare)(info, dc)

  /**
   * Execute SQL on connection and return prepared statement. Closes the
   * statement in a bracket.
   */
  def prepareSingle(
    sql: String,
    prepare: Prepare = identityPrepare
  )(info: ExecutionInfo, dc: Runner): QCIO[PreparedStatement] =
    (for {
      conn <- ZIO.service[Session]
      stmt <- ZIO.attempt(conn.prepareStatement(sql))
      ps <- ZIO.attempt {
              val (params, ps) = prepare(stmt, conn)
              logger.logQuery(sql, params)
              ps
            }
    } yield ps).refineToOrDie[SQLException]

  def prepareBatchAction(groups: List[BatchGroup])(info: ExecutionInfo, dc: Runner): PrepareBatchActionResult =
    ZIO
      .collectAll[Connection, Throwable, PrepareRow, List] {
        val batches = groups.flatMap { case BatchGroup(sql, prepares, _) =>
          prepares.map(sql -> _)
        }
        batches.map { case (sql, prepare) =>
          prepareSingle(sql, prepare)(info, dc)
        }
      }
      .refineToOrDie[SQLException]
}
