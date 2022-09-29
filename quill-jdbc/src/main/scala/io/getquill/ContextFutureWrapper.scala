package io.getquill

import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.monad.ScalaFutureIOMonad
import io.getquill.context.jdbc.{ JdbcContext, JdbcContextBase }
import io.getquill.context.ContextVerbTranslate
import io.getquill.context.sql.SqlContext
import scala.util.Try
import java.util.Date
import java.time.LocalDate
import java.util.UUID
import io.getquill.context.ExecutionInfo
import scala.concurrent.{ Future, ExecutionContext }

class ContextFutureWrapper[+Dialect <: SqlIdiom, +Naming <: NamingStrategy](
  val wrappedContext: JdbcContext[Dialect, Naming] with JdbcContextBase[Dialect, Naming]
) extends SqlContext[Dialect, Naming] with ContextVerbTranslate with ScalaFutureIOMonad {

  override type Index = wrappedContext.Index
  override type ResultRow = wrappedContext.ResultRow
  override type BaseNullChecker = wrappedContext.BaseNullChecker
  override type NullChecker = wrappedContext.NullChecker
  override type Encoder[T] = wrappedContext.Encoder[T]
  override type Decoder[T] = wrappedContext.Decoder[T]
  override type PrepareRow = wrappedContext.PrepareRow
  override type Runner = wrappedContext.Runner
  override type Session = wrappedContext.Session
  override type BatchGroup = wrappedContext.BatchGroup
  override type BatchGroupReturning = wrappedContext.BatchGroupReturning

  override def close(): Unit = wrappedContext.close

  override implicit def mappedEncoder[I, O](implicit mapped: MappedEncoding[I, O], encoder: Encoder[O]): Encoder[I] = wrappedContext.mappedEncoder

  override implicit def mappedDecoder[I, O](implicit mapped: MappedEncoding[I, O], decoder: Decoder[I]): Decoder[O] = wrappedContext.mappedDecoder

  override def probe(statement: String): Try[Boolean] = wrappedContext.probe(statement)

  override def idiom: Dialect = wrappedContext.idiom

  override def naming: Naming = wrappedContext.naming

  override implicit val nullChecker: NullChecker = wrappedContext.nullChecker

  override implicit def optionDecoder[T](implicit d: Decoder[T]): Decoder[Option[T]] = wrappedContext.optionDecoder

  override implicit def optionEncoder[T](implicit d: Encoder[T]): Encoder[Option[T]] = wrappedContext.optionEncoder

  override implicit val stringDecoder: Decoder[String] = wrappedContext.stringDecoder

  override implicit val bigDecimalDecoder: Decoder[BigDecimal] = wrappedContext.bigDecimalDecoder

  override implicit val booleanDecoder: Decoder[Boolean] = wrappedContext.booleanDecoder

  override implicit val byteDecoder: Decoder[Byte] = wrappedContext.byteDecoder

  override implicit val shortDecoder: Decoder[Short] = wrappedContext.shortDecoder

  override implicit val intDecoder: Decoder[Int] = wrappedContext.intDecoder

  override implicit val longDecoder: Decoder[Long] = wrappedContext.longDecoder

  override implicit val floatDecoder: Decoder[Float] = wrappedContext.floatDecoder

  override implicit val doubleDecoder: Decoder[Double] = wrappedContext.doubleDecoder

  override implicit val byteArrayDecoder: Decoder[Array[Byte]] = wrappedContext.byteArrayDecoder

  override implicit val dateDecoder: Decoder[Date] = wrappedContext.dateDecoder

  override implicit val localDateDecoder: Decoder[LocalDate] = wrappedContext.localDateDecoder

  override implicit val uuidDecoder: Decoder[UUID] = wrappedContext.uuidDecoder

  override implicit val stringEncoder: Encoder[String] = wrappedContext.stringEncoder

  override implicit val bigDecimalEncoder: Encoder[BigDecimal] = wrappedContext.bigDecimalEncoder

  override implicit val booleanEncoder: Encoder[Boolean] = wrappedContext.booleanEncoder

  override implicit val byteEncoder: Encoder[Byte] = wrappedContext.byteEncoder

  override implicit val shortEncoder: Encoder[Short] = wrappedContext.shortEncoder

  override implicit val intEncoder: Encoder[Int] = wrappedContext.intEncoder

  override implicit val longEncoder: Encoder[Long] = wrappedContext.longEncoder

  override implicit val floatEncoder: Encoder[Float] = wrappedContext.floatEncoder

  override implicit val doubleEncoder: Encoder[Double] = wrappedContext.doubleEncoder

  override implicit val byteArrayEncoder: Encoder[Array[Byte]] = wrappedContext.byteArrayEncoder

  override implicit val dateEncoder: Encoder[Date] = wrappedContext.dateEncoder

  override implicit val localDateEncoder: Encoder[LocalDate] = wrappedContext.localDateEncoder

  override implicit val uuidEncoder: Encoder[UUID] = wrappedContext.uuidEncoder

  def executeQuery[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(info: ExecutionInfo, dc: Runner)(implicit ec: ExecutionContext): Future[List[T]] =
    Future.successful(wrappedContext.executeQuery(sql, prepare, extractor)(info, dc))

  def executeQuerySingle[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(info: ExecutionInfo, dc: Runner)(implicit ec: ExecutionContext): Future[T] =
    Future.successful(handleSingleResult(sql, wrappedContext.executeQuery(sql, prepare, extractor)(info, dc)))

  def executeAction(sql: String, prepare: Prepare = identityPrepare)(info: ExecutionInfo, dc: Runner)(implicit ec: ExecutionContext): Future[Long] =
    Future.successful(wrappedContext.executeAction(sql, prepare)(info, dc))

  def executeActionReturning[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T], returningAction: ReturnAction)(info: ExecutionInfo, dc: Runner)(implicit ec: ExecutionContext): Future[T] =
    Future.successful(handleSingleResult(sql, wrappedContext.executeActionReturningMany[T](sql, prepare, extractor, returningAction)(info, dc)))

  def executeActionReturningMany[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T], returningAction: ReturnAction)(info: ExecutionInfo, dc: Runner)(implicit ec: ExecutionContext): Future[List[T]] =
    Future.successful(wrappedContext.executeActionReturningMany(sql, prepare, extractor, returningAction)(info, dc))

  def executeBatchAction(groups: List[BatchGroup])(info: ExecutionInfo, dc: Runner)(implicit ec: ExecutionContext): Future[List[Long]] =
    Future.successful(wrappedContext.executeBatchAction(groups)(info, dc))

  def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: Extractor[T])(info: ExecutionInfo, dc: Runner)(implicit ec: ExecutionContext): Future[List[T]] =
    Future.successful(wrappedContext.executeBatchActionReturning(groups, extractor)(info, dc))

  override private[getquill] def prepareParams(statement: String, prepare: Prepare): Seq[String] = wrappedContext.prepareParams(statement, prepare)
}
