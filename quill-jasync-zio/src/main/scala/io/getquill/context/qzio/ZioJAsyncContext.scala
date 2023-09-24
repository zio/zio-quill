package io.getquill.context.qzio

import com.github.jasync.sql.db.{ConcreteConnection, QueryResult, RowData}
import io.getquill.context.sql.SqlContext
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.context.{Context, ContextVerbTranslate, ExecutionInfo}
import io.getquill.util.ContextLogger
import io.getquill.{NamingStrategy, ReturnAction}
import kotlin.jvm.functions.Function1
import zio.{RIO, ZIO}

import java.time.ZoneId
import scala.jdk.CollectionConverters._
import scala.language.implicitConversions
import scala.util.Try

abstract class ZioJAsyncContext[D <: SqlIdiom, +N <: NamingStrategy, C <: ConcreteConnection](
  val idiom: D,
  val naming: N
) extends Context[D, N]
    with ContextVerbTranslate
    with SqlContext[D, N]
    with Decoders
    with Encoders
    with ZIOMonad {

  protected val dateTimeZone = ZoneId.systemDefault()

  private val logger = ContextLogger(classOf[ZioJAsyncContext[_, _, _]])

  override type PrepareRow = Seq[Any]
  override type ResultRow  = RowData
  override type Session    = Unit

  override type Result[T]                        = RIO[ZioJAsyncConnection, T]
  override type RunQueryResult[T]                = Seq[T]
  override type RunQuerySingleResult[T]          = T
  override type RunActionResult                  = Long
  override type RunActionReturningResult[T]      = T
  override type RunBatchActionResult             = Seq[Long]
  override type RunBatchActionReturningResult[T] = Seq[T]
  override type DecoderSqlType                   = SqlTypes.SqlTypes
  type DatasourceContext                         = Unit

  override type NullChecker = ZioJasyncNullChecker
  class ZioJasyncNullChecker extends BaseNullChecker {
    override def apply(index: Int, row: RowData): Boolean =
      row.get(index) == null
  }
  implicit val nullChecker: NullChecker = new ZioJasyncNullChecker()

  implicit def toKotlinFunction[T, R](f: T => R): Function1[T, R] = new Function1[T, R] {
    override def invoke(t: T): R = f(t)
  }

  override def close: Unit =
    // nothing to close since pool is in env
    ()

  protected def extractActionResult[O](returningAction: ReturnAction, extractor: Extractor[O])(
    result: QueryResult
  ): List[O]

  protected def expandAction(sql: String, returningAction: ReturnAction): String = sql

  def probe(sql: String): Try[_] =
    Try(()) // need to address that

  def transaction[R <: ZioJAsyncConnection, T](action: RIO[R, T]): RIO[R, T] =
    ZIO.environmentWithZIO[ZioJAsyncConnection](_.get.transaction(action))

  override def performIO[T](io: IO[T, _], transactional: Boolean = false): RIO[ZioJAsyncConnection, T] =
    if (transactional) {
      transaction(super.performIO(io))
    } else {
      super.performIO(io)
    }

  def executeQuery[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(
    info: ExecutionInfo,
    dc: DatasourceContext
  ): RIO[ZioJAsyncConnection, List[T]] = {
    val (params, values) = prepare(Nil, ())
    logger.logQuery(sql, params)
    ZioJAsyncConnection
      .sendPreparedStatement(sql, values)
      .map(_.getRows.asScala.iterator.map(row => extractor(row, ())).toList)
  }

  def executeQuerySingle[T](
    sql: String,
    prepare: Prepare = identityPrepare,
    extractor: Extractor[T] = identityExtractor
  )(info: ExecutionInfo, dc: DatasourceContext): RIO[ZioJAsyncConnection, T] =
    executeQuery(sql, prepare, extractor)(info, dc).map(handleSingleResult(sql, _))

  def executeAction[T](
    sql: String,
    prepare: Prepare = identityPrepare
  )(info: ExecutionInfo, dc: DatasourceContext): Result[Long] = {
    val (params, values) = prepare(Nil, ())
    logger.logQuery(sql, params)
    ZioJAsyncConnection.sendPreparedStatement(sql, values).map(_.getRowsAffected)
  }

  def executeActionReturning[T](
    sql: String,
    prepare: Prepare = identityPrepare,
    extractor: Extractor[T],
    returningAction: ReturnAction
  )(info: ExecutionInfo, dc: DatasourceContext): RIO[ZioJAsyncConnection, T] =
    executeActionReturningMany[T](sql, prepare, extractor, returningAction)(info, dc).map(handleSingleResult(sql, _))

  def executeActionReturningMany[T](
    sql: String,
    prepare: Prepare = identityPrepare,
    extractor: Extractor[T],
    returningAction: ReturnAction
  )(info: ExecutionInfo, dc: DatasourceContext): RIO[ZioJAsyncConnection, List[T]] = {
    val expanded         = expandAction(sql, returningAction)
    val (params, values) = prepare(Nil, ())
    logger.logQuery(sql, params)
    ZioJAsyncConnection
      .sendPreparedStatement(expanded, values)
      .map(extractActionResult(returningAction, extractor))
  }

  def executeBatchAction(
    groups: List[BatchGroup]
  )(info: ExecutionInfo, dc: DatasourceContext): RIO[ZioJAsyncConnection, List[Long]] =
    ZIO
      .foreach(groups) { group =>
        ZIO
          .foldLeft(group.prepare)(List.newBuilder[Long]) { case (acc, prepare) =>
            executeAction(group.string, prepare)(info, dc).map(acc += _)
          }
          .map(_.result())
      }
      .map(_.flatten.toList)

  def executeBatchActionReturning[T](
    groups: List[BatchGroupReturning],
    extractor: Extractor[T]
  )(info: ExecutionInfo, dc: DatasourceContext): RIO[ZioJAsyncConnection, List[T]] =
    ZIO
      .foreach(groups) { case BatchGroupReturning(sql, column, prepare) =>
        ZIO
          .foldLeft(prepare)(List.newBuilder[T]) { case (acc, prepare) =>
            executeActionReturning(sql, prepare, extractor, column)(info, dc).map(acc += _)
          }
          .map(_.result())
      }
      .map(_.flatten.toList)

  override private[getquill] def prepareParams(statement: String, prepare: Prepare): Seq[String] =
    prepare(Nil, ())._2.map(prepareParam)

}
