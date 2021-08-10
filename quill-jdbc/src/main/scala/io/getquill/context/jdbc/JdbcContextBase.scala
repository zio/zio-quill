package io.getquill.context.jdbc

import io.getquill._
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.context.{ ExecutionInfo, PrepareContext, StagedPrepare }

import java.sql._

trait JdbcContextBase[Dialect <: SqlIdiom, Naming <: NamingStrategy] extends JdbcContextSimplified[Dialect, Naming]
  with StagedPrepare {

  // Need to re-define these here or they conflict with staged-prepare imported types
  override type PrepareQueryResult = Connection => Result[PreparedStatement]
  override type PrepareActionResult = Connection => Result[PreparedStatement]
  override type PrepareBatchActionResult = Connection => Result[List[PreparedStatement]]

  def constructPrepareQuery(f: Connection => Result[PreparedStatement]): Connection => Result[PreparedStatement] = f
  def constructPrepareAction(f: Connection => Result[PreparedStatement]): Connection => Result[PreparedStatement] = f
  def constructPrepareBatchAction(f: Connection => Result[List[PreparedStatement]]): Connection => Result[List[PreparedStatement]] = f
}

trait JdbcContextSimplified[Dialect <: SqlIdiom, Naming <: NamingStrategy]
  extends JdbcRunContext[Dialect, Naming] with PrepareContext {

  override type PrepareQueryResult = Connection => Result[PreparedStatement]
  override type PrepareActionResult = Connection => Result[PreparedStatement]
  override type PrepareBatchActionResult = Connection => Result[List[PreparedStatement]]

  import effect._
  def constructPrepareQuery(f: Connection => Result[PreparedStatement]): PrepareQueryResult
  def constructPrepareAction(f: Connection => Result[PreparedStatement]): PrepareActionResult
  def constructPrepareBatchAction(f: Connection => Result[List[PreparedStatement]]): PrepareBatchActionResult

  def prepareQuery(sql: String, prepare: Prepare = identityPrepare)(executionInfo: ExecutionInfo, dc: DatasourceContext): PrepareQueryResult =
    constructPrepareQuery(prepareSingle(sql, prepare)(executionInfo, dc))

  def prepareAction(sql: String, prepare: Prepare = identityPrepare)(executionInfo: ExecutionInfo, dc: DatasourceContext): PrepareActionResult =
    constructPrepareAction(prepareSingle(sql, prepare)(executionInfo, dc))

  def prepareSingle(sql: String, prepare: Prepare = identityPrepare)(executionInfo: ExecutionInfo, dc: DatasourceContext): Connection => Result[PreparedStatement] =
    (conn: Connection) => wrap {
      val (params, ps) = prepare(conn.prepareStatement(sql), conn)
      logger.logQuery(sql, params)
      ps
    }

  def prepareBatchAction(groups: List[BatchGroup])(executionInfo: ExecutionInfo, dc: DatasourceContext): PrepareBatchActionResult =
    constructPrepareBatchAction {
      (session: Connection) =>
        seq {
          val batches = groups.flatMap {
            case BatchGroup(sql, prepares) =>
              prepares.map(sql -> _)
          }
          batches.map {
            case (sql, prepare) =>
              val prepareSql = prepareSingle(sql, prepare)(executionInfo, dc)
              prepareSql(session)
          }
        }
    }

}
