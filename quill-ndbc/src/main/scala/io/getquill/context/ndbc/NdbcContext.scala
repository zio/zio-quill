package io.getquill.context.ndbc

import java.time.ZoneOffset
import java.util.Iterator
import java.util.function.Supplier

import scala.annotation.tailrec
import scala.util.Try

import org.slf4j.LoggerFactory.getLogger

import com.typesafe.scalalogging.Logger

import io.getquill.{ NamingStrategy, ReturnAction }
import io.getquill.context.sql.SqlContext
import io.getquill.context.sql.idiom.SqlIdiom
import io.trane.future.scala.{ Future, toJavaFuture, toScalaFuture }
import io.trane.ndbc.{ DataSource, PreparedStatement, Row }

abstract class NdbcContext[I <: SqlIdiom, N <: NamingStrategy, P <: PreparedStatement, R <: Row](
  val idiom: I, val naming: N, dataSource: DataSource[P, R]
)
  extends SqlContext[I, N] {

  private val logger = Logger(getLogger(classOf[NdbcContext[_, _, _, _]]))

  override type PrepareRow = P
  override type ResultRow = R

  override type Result[T] = Future[T]
  override type RunQueryResult[T] = List[T]
  override type RunQuerySingleResult[T] = T
  override type RunActionResult = Long
  override type RunActionReturningResult[T] = T
  override type RunBatchActionResult = List[Long]
  override type RunBatchActionReturningResult[T] = List[T]

  protected val zoneOffset: ZoneOffset = ZoneOffset.UTC

  protected def createPreparedStatement(sql: String): P
  protected def expandAction(sql: String, returningAction: ReturnAction) = sql

  def close() = {
    dataSource.close()
    ()
  }

  def probe(sql: String) = Try(dataSource.query(sql))

  def transaction[T](f: => Future[T]): Future[T] = {
    dataSource.transactional(new Supplier[io.trane.future.Future[T]] {
      override def get = f.toJava
    }).toScala
  }

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

  def executeActionReturning[O](sql: String, prepare: Prepare = identityPrepare, extractor: R => O, returningAction: ReturnAction): Future[O] = {
    val expanded = expandAction(sql, returningAction)
    executeQuerySingle(expanded, prepare, extractor)
  }

  def executeBatchAction(groups: List[BatchGroup]): Future[List[Long]] =
    Future.sequence {
      groups.map {
        case BatchGroup(sql, prepare) =>
          prepare.foldLeft(Future.successful(Vector.empty[Long])) {
            case (acc, prepare) =>
              acc.flatMap { vec =>
                executeAction(sql, prepare).map(vec :+ _)
              }
          }
      }
    }.map(_.flatten.toList)

  def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: R => T): Future[List[T]] =
    Future.sequence {
      groups.map {
        case BatchGroupReturning(sql, column, prepare) =>
          prepare.foldLeft(Future.successful(Vector.empty[T])) {
            case (acc, prepare) =>
              acc.flatMap { vec =>
                executeActionReturning(sql, prepare, extractor, column).map(vec :+ _)
              }
          }
      }
    }.map(_.flatten.toList)

  @tailrec
  private def extractResult[T](rs: Iterator[R], extractor: R => T, acc: List[T] = Nil): List[T] =
    if (rs.hasNext)
      extractResult(rs, extractor, extractor(rs.next()) :: acc)
    else
      acc.reverse
}