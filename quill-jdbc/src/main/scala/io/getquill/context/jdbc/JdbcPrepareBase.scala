package io.getquill.context.jdbc

import java.sql.{ Connection, PreparedStatement }

import io.getquill.NamingStrategy
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.context.{ Context, ContextEffect }
import io.getquill.util.ContextLogger

trait JdbcPrepareBase[Dialect <: SqlIdiom, Naming <: NamingStrategy]
  extends Context[Dialect, Naming] {

  private[getquill] val logger = ContextLogger(classOf[JdbcPrepareBase[_, _]])

  type PrepareResult
  type PrepareBatchResult
  override type PrepareRow = PreparedStatement

  def prepareQuery[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): PrepareResult
  def prepareAction(sql: String, prepare: Prepare = identityPrepare): PrepareResult
  def prepareSingle(sql: String, prepare: Prepare = identityPrepare): PrepareResult
  def prepareBatchAction(groups: List[BatchGroup]): PrepareBatchResult

  protected val effect: ContextEffect[Result]
  import effect._

  protected def prepareInternal(conn: Connection, sql: String, prepare: Prepare) =
    wrap {
      val (params, ps) = prepare(conn.prepareStatement(sql))
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
