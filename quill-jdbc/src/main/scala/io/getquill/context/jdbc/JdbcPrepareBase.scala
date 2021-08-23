package io.getquill.context.jdbc

import java.sql.{ Connection, PreparedStatement }
import io.getquill.NamingStrategy
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.context.{ Context, ContextEffect, ExecutionInfo }
import io.getquill.util.ContextLogger

trait JdbcPrepareBase[Dialect <: SqlIdiom, Naming <: NamingStrategy]
  extends Context[Dialect, Naming] {

  private[getquill] val logger = ContextLogger(classOf[JdbcPrepareBase[_, _]])

  type PrepareResult
  type PrepareBatchResult
  type DatasourceContext = Unit
  override type PrepareRow = PreparedStatement
  override type Session = Connection

  def prepareQuery(sql: String, prepare: Prepare = identityPrepare)(executionInfo: ExecutionInfo, dc: DatasourceContext): PrepareResult
  def prepareAction(sql: String, prepare: Prepare = identityPrepare)(executionInfo: ExecutionInfo, dc: DatasourceContext): PrepareResult
  def prepareSingle(sql: String, prepare: Prepare = identityPrepare)(executionInfo: ExecutionInfo, dc: DatasourceContext): PrepareResult
  def prepareBatchAction(groups: List[BatchGroup])(executionInfo: ExecutionInfo, dc: DatasourceContext): PrepareBatchResult

  protected val effect: ContextEffect[Result]
  import effect._

  protected def prepareInternal(conn: Connection, sql: String, prepare: Prepare) =
    wrap {
      val (params, ps) = prepare(conn.prepareStatement(sql), conn)
      logger.logQuery(sql, params)
      ps
    }

  protected def prepareBatchInternal(conn: Connection, groups: List[BatchGroup], prepareSingle: (String, Prepare, Connection) => Result[PreparedStatement]) =
    seq {
      val batches = groups.flatMap {
        case BatchGroup(sql, prepares) =>
          prepares.map(sql -> _)
      }
      batches.map {
        case (sql, prepare) =>
          val singleResult = prepareSingle(sql, prepare, conn)
          singleResult
      }
    }
}
