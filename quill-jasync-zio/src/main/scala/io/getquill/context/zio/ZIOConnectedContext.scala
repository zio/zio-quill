package io.getquill.context.zio

import com.github.jasync.sql.db.{ ConcreteConnection, RowData }
import io.getquill.context.sql.SqlContext
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.context.{ Context, ExecutionInfo, TranslateContext }
import io.getquill.{ NamingStrategy, ReturnAction }
import zio.{ Has, Tag, ZIO, ZLayer }

import java.util.UUID
import scala.util.Try

class ZIOConnectedContext[D <: SqlIdiom, N <: NamingStrategy, C <: ConcreteConnection](val context: ZIOJAsyncContext[D, N, C], val connection: ZIOJAsyncConnection)
  extends Context[D, N]
  with TranslateContext
  with SqlContext[D, N]
  with Decoders
  with Encoders {

  override type PrepareRow = Seq[Any]
  override type ResultRow = RowData
  override type Session = Unit

  override type Result[T] = ZIO[Any, Throwable, T]
  override type RunQueryResult[T] = Seq[T]
  override type RunQuerySingleResult[T] = T
  override type RunActionResult = Long
  override type RunActionReturningResult[T] = T
  override type RunBatchActionResult = Seq[Long]
  override type RunBatchActionReturningResult[T] = Seq[T]
  override type DecoderSqlType = SqlTypes.SqlTypes

  type DatasourceContext = Unit

  override def close: Unit = {
    context.close
    ()
  }

  final def idiom: D = context.idiom
  final def naming: N = context.naming

  final def probe(sql: String): Try[_] =
    Try(()) //need to address that

  def transaction[T](action: Result[T]): ZIO[Any, Throwable, T] = {
    context.transaction(action).provide(Has(connection))
  }

  def executeQuery[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(info: ExecutionInfo, dc: DatasourceContext): ZIO[Any, Throwable, List[T]] = {
    context.executeQuery(sql, prepare, extractor)(info, dc).provide(Has(connection))
  }

  def executeQuerySingle[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(info: ExecutionInfo, dc: DatasourceContext): ZIO[Any, Throwable, T] =
    executeQuery(sql, prepare, extractor)(info, dc).map(handleSingleResult)

  def executeAction[T](sql: String, prepare: Prepare = identityPrepare)(info: ExecutionInfo, dc: DatasourceContext): Result[Long] = {
    context.executeAction(sql, prepare)(info, dc).provide(Has(connection))
  }

  def executeActionReturning[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T], returningAction: ReturnAction)(info: ExecutionInfo, dc: DatasourceContext): ZIO[Any, Throwable, T] = {
    context.executeActionReturning(sql, prepare, extractor, returningAction)(info, dc).provide(Has(connection))
  }

  def executeBatchAction(groups: List[BatchGroup])(info: ExecutionInfo, dc: DatasourceContext): ZIO[Any, Throwable, List[Long]] =
    context.executeBatchAction(groups.asInstanceOf[List[context.BatchGroup]])(info, dc).provide(Has(connection))

  def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: Extractor[T])(info: ExecutionInfo, dc: DatasourceContext): ZIO[Any, Throwable, List[T]] =
    context.executeBatchActionReturning(groups.asInstanceOf[List[context.BatchGroupReturning]], extractor)(info, dc).provide(Has(connection))

  override private[getquill] def prepareParams(statement: String, prepare: Prepare): Seq[String] =
    context.prepareParams(statement, prepare)

  implicit val uuidEncoder: Encoder[UUID] = context.uuidEncoder.asInstanceOf[Encoder[UUID]]

  implicit val uuidDecoder: Decoder[UUID] = context.uuidDecoder.asInstanceOf[Decoder[UUID]]

}

object ZIOConnectedContext {
  def live[D <: SqlIdiom: Tag, N <: NamingStrategy: Tag, C <: ConcreteConnection: Tag](context: ZIOJAsyncContext[D, N, C]): ZLayer[Has[ZIOJAsyncConnection], Nothing, Has[ZIOConnectedContext[D, N, C]]] =
    ZLayer.fromService((conn: ZIOJAsyncConnection) => new ZIOConnectedContext[D, N, C](context, conn))
}
