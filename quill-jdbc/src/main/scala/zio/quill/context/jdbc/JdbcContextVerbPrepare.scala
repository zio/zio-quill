package zio.quill.context.jdbc

import zio.quill._
import zio.quill.context.sql.idiom.SqlIdiom
import zio.quill.context.{ContextVerbPrepare, ExecutionInfo}
import zio.quill.util.ContextLogger

import java.sql._

trait JdbcContextVerbPrepare[+Dialect <: SqlIdiom, +Naming <: NamingStrategy]
    extends ContextVerbPrepare
    with JdbcContextTypes[Dialect, Naming] {

  override type PrepareQueryResult       = Connection => Result[PreparedStatement]
  override type PrepareActionResult      = Connection => Result[PreparedStatement]
  override type PrepareBatchActionResult = Connection => Result[List[PreparedStatement]]

  def constructPrepareQuery(f: Connection => Result[PreparedStatement]): PrepareQueryResult
  def constructPrepareAction(f: Connection => Result[PreparedStatement]): PrepareActionResult
  def constructPrepareBatchAction(f: Connection => Result[List[PreparedStatement]]): PrepareBatchActionResult

  private[quill] val logger = ContextLogger(classOf[JdbcContext[_, _]])

  def wrap[T](t: => T): Result[T]
  def push[A, B](result: Result[A])(f: A => B): Result[B]
  def seq[A](list: List[Result[A]]): Result[List[A]]

  def prepareQuery(sql: String, prepare: Prepare = identityPrepare)(
    executionInfo: ExecutionInfo,
    dc: Runner
  ): PrepareQueryResult =
    constructPrepareQuery(prepareSingle(sql, prepare)(executionInfo, dc))

  def prepareAction(sql: String, prepare: Prepare = identityPrepare)(
    executionInfo: ExecutionInfo,
    dc: Runner
  ): PrepareActionResult =
    constructPrepareAction(prepareSingle(sql, prepare)(executionInfo, dc))

  def prepareSingle(
    sql: String,
    prepare: Prepare = identityPrepare
  )(executionInfo: ExecutionInfo, dc: Runner): Connection => Result[PreparedStatement] =
    (conn: Connection) =>
      wrap {
        val (params, ps) = prepare(conn.prepareStatement(sql), conn)
        logger.logQuery(sql, params)
        ps
      }

  def prepareBatchAction(groups: List[BatchGroup])(executionInfo: ExecutionInfo, dc: Runner): PrepareBatchActionResult =
    constructPrepareBatchAction { (session: Connection) =>
      seq {
        val batches = groups.flatMap { case BatchGroup(sql, prepares, _) =>
          prepares.map(sql -> _)
        }
        batches.map { case (sql, prepare) =>
          val prepareSql = prepareSingle(sql, prepare)(executionInfo, dc)
          prepareSql(session)
        }
      }
    }

}
