package io.getquill.context.ndbc

import scala.annotation.tailrec
import scala.util.Try

import org.slf4j.LoggerFactory

import com.typesafe.scalalogging.Logger

import io.getquill.NamingStrategy
import io.getquill.context.sql.SqlContext
import io.getquill.context.sql.idiom.SqlIdiom
import io.trane.future.scala._
import io.trane.ndbc.DataSource
import io.trane.ndbc.Row
import java.time.ZoneOffset
import io.trane.ndbc.PreparedStatement

abstract class BaseNdbcContext[Dialect <: SqlIdiom, Naming <: NamingStrategy, P <: PreparedStatement, R <: Row](
  val idiom: Dialect, val naming: Naming, dataSource: DataSource[P, R]
)
  extends SqlContext[Dialect, Naming] {

  protected val zoneOffset: ZoneOffset = ZoneOffset.UTC

  private val logger: Logger =
    Logger(LoggerFactory.getLogger(classOf[BaseNdbcContext[_, _, _, _]]))

  override type PrepareRow = P

  override type ResultRow = R

  override type Result[T] = Future[T]
  override type RunQueryResult[T] = List[T]
  override type RunQuerySingleResult[T] = T
  override type RunActionResult = Long
  override type RunActionReturningResult[T] = T
  override type RunBatchActionResult = List[Long]
  override type RunBatchActionReturningResult[T] = List[T]

  def close() = {
    dataSource.close()
    ()
  }

  def probe(sql: String) =
    Try(dataSource.query(sql))

  def transaction[T](f: => Future[T]): Future[T] =
    dataSource.transactional(() => f.toJava).toScala

  def executeQuery[T](sql: String, prepare: Prepare = identityPrepare, extractor: R => T = identity[R] _): Future[List[T]] = {
    val ps = prepare(createPreparedStatement(sql))._2
    logger.debug(ps.toString())
    dataSource.query(ps).toScala.map { rs =>
      extractResult(rs.iterator, extractor)
    }
  }

  def executeQuerySingle[T](sql: String, prepare: Prepare = identityPrepare, extractor: R => T = identity[R] _): Future[T] =
    executeQuery(sql, prepare, extractor).map(handleSingleResult(_))

  def executeAction[T](sql: String, prepare: Prepare = identityPrepare): Future[Long] = {
    val ps = prepare(createPreparedStatement(sql))._2
    logger.debug(ps.toString())
    dataSource.execute(ps).toScala.map(_.longValue())
  }

  def executeActionReturning[O](sql: String, prepare: Prepare = identityPrepare, extractor: R => O, returningColumn: String): Future[O] =
    executeQuerySingle(s"$sql RETURNING $returningColumn", prepare, extractor)

  def executeBatchAction(groups: List[BatchGroup]): Future[List[Long]] =
    Future.sequence {
      groups.map {
        case BatchGroup(sql, prepare) =>
          prepare.foldLeft(Future.successful(List.empty[Long])) {
            case (acc, prepare) =>
              acc.flatMap { list =>
                executeAction(sql, prepare).map(list :+ _)
              }
          }
      }
    }.map(_.flatten.toList)

  def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: R => T): Future[List[T]] =
    Future.sequence {
      groups.map {
        case BatchGroupReturning(sql, column, prepare) =>
          prepare.foldLeft(Future.successful(List.empty[T])) {
            case (acc, prepare) =>
              acc.flatMap { list =>
                executeActionReturning(sql, prepare, extractor, column).map(list :+ _)
              }
          }
      }
    }.map(_.flatten.toList)

  @tailrec
  private def extractResult[T](rs: java.util.Iterator[R], extractor: R => T, acc: List[T] = List()): List[T] =
    if (rs.hasNext)
      extractResult(rs, extractor, extractor(rs.next()) :: acc)
    else
      acc.reverse

  protected def createPreparedStatement(sql: String): P
}
