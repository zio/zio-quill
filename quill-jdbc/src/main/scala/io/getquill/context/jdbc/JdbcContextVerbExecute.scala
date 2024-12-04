package io.getquill.context.jdbc

import io.getquill.ReturnAction.{ReturnColumns, ReturnNothing, ReturnRecord}
import io.getquill.context.ExecutionInfo
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.util.ContextLogger
import io.getquill.{NamingStrategy, ReturnAction}

import java.sql.{Connection, ResultSet, Statement}

trait JdbcContextVerbExecute[+Dialect <: SqlIdiom, +Naming <: NamingStrategy]
    extends JdbcContextTypes[Dialect, Naming] {

  // These type overrides are not required for JdbcRunContext in Scala2-Quill but it's a typing error. It only works
  // because executeQuery is not actually defined in Context.scala therefore typing doesn't have
  // to be correct on the base-level. Same issue with RunActionResult and others
  override type RunQueryResult[T]                = List[T]
  override type RunQuerySingleResult[T]          = T
  override type RunActionResult                  = Long
  override type RunActionReturningResult[T]      = T
  override type RunBatchActionResult             = List[Long]
  override type RunBatchActionReturningResult[T] = List[T]

  private val logger = ContextLogger(classOf[JdbcContextVerbExecute[_, _]])

  def wrap[T](t: => T): Result[T]
  def push[A, B](result: Result[A])(f: A => B): Result[B]
  def seq[A](list: List[Result[A]]): Result[List[A]]

  protected def withConnection[T](f: Connection => Result[T]): Result[T]
  protected def withConnectionWrapped[T](f: Connection => T): Result[T] =
    withConnection(conn => wrap(f(conn)))

  def executeAction(sql: String, prepare: Prepare = identityPrepare)(info: ExecutionInfo, dc: Runner): Result[Long] =
    withConnectionWrapped { conn =>
      val (params, ps) = prepare(conn.prepareStatement(sql), conn)
      logger.logQuery(sql, params)
      ps.executeUpdate().toLong
    }

  def executeQuery[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(
    info: ExecutionInfo,
    dc: Runner
  ): Result[List[T]] =
    withConnectionWrapped { conn =>
      val (params, ps) = prepare(conn.prepareStatement(sql), conn)
      logger.logQuery(sql, params)
      val rs = ps.executeQuery()
      extractResult(rs, conn, extractor)
    }

  def executeQuerySingle[T](
    sql: String,
    prepare: Prepare = identityPrepare,
    extractor: Extractor[T] = identityExtractor
  )(info: ExecutionInfo, dc: Runner): Result[T] =
    handleSingleWrappedResult(sql, executeQuery(sql, prepare, extractor)(info, dc))

  def executeActionReturning[O](
    sql: String,
    prepare: Prepare = identityPrepare,
    extractor: Extractor[O],
    returningBehavior: ReturnAction
  )(info: ExecutionInfo, dc: Runner): Result[O] =
    push(executeActionReturningMany(sql, prepare, extractor, returningBehavior)(info, dc))(handleSingleResult(sql, _))

  def executeActionReturningMany[O](
    sql: String,
    prepare: Prepare = identityPrepare,
    extractor: Extractor[O],
    returningBehavior: ReturnAction
  )(info: ExecutionInfo, dc: Runner): Result[List[O]] =
    withConnectionWrapped { conn =>
      val (params, ps) = prepare(prepareWithReturning(sql, conn, returningBehavior), conn)
      logger.logQuery(sql, params)
      ps.executeUpdate()
      extractResult(ps.getGeneratedKeys, conn, extractor)
    }

  protected def prepareWithReturning(sql: String, conn: Connection, returningBehavior: ReturnAction) =
    returningBehavior match {
      case ReturnRecord           => conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
      case ReturnColumns(columns) => conn.prepareStatement(sql, columns.toArray)
      case ReturnNothing          => conn.prepareStatement(sql)
    }

  def executeBatchAction(groups: List[BatchGroup])(info: ExecutionInfo, dc: Runner): Result[List[Long]] =
    withConnectionWrapped { conn =>
      groups.flatMap { case BatchGroup(sql, prepare, _) =>
        val ps = conn.prepareStatement(sql)
        logger.underlying.debug("Batch: {}", sql)
        prepare.foreach { f =>
          val (params, _) = f(ps, conn)
          logger.logBatchItem(sql, params)
          ps.addBatch()
        }
        ps.executeBatch().map(_.toLong)
      }
    }

  def executeBatchActionReturning[T](
    groups: List[BatchGroupReturning],
    extractor: Extractor[T]
  )(info: ExecutionInfo, dc: Runner): Result[List[T]] =
    withConnectionWrapped { conn =>
      groups.flatMap { case BatchGroupReturning(sql, returningBehavior, prepare) =>
        val ps = prepareWithReturning(sql, conn, returningBehavior)
        logger.underlying.debug("Batch: {}", sql)
        prepare.foreach { f =>
          val (params, _) = f(ps, conn)
          logger.logBatchItem(sql, params)
          ps.addBatch()
        }
        ps.executeBatch()
        extractResult(ps.getGeneratedKeys, conn, extractor)
      }
    }

  protected def handleSingleWrappedResult[T](sql: String, list: Result[List[T]]): Result[T] =
    push(list)(handleSingleResult(sql, _))

  private[getquill] final def extractResult[T](rs: ResultSet, conn: Connection, extractor: Extractor[T]): List[T] =
    ResultSetExtractor(rs, conn, extractor)
}
