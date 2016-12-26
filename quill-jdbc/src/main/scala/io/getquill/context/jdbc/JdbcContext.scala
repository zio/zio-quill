package io.getquill.context.jdbc

import java.io.Closeable
import java.sql.{ Connection, PreparedStatement, ResultSet }
import javax.sql.DataSource

import com.typesafe.scalalogging.Logger
import io.getquill.context.sql.SqlContext
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.NamingStrategy
import org.slf4j.LoggerFactory

import scala.annotation.tailrec
import scala.util.{ DynamicVariable, Try }
import scala.util.control.NonFatal

abstract class JdbcContext[Dialect <: SqlIdiom, Naming <: NamingStrategy](dataSource: DataSource with Closeable)
  extends SqlContext[Dialect, Naming]
  with Encoders
  with Decoders {

  private val logger: Logger =
    Logger(LoggerFactory.getLogger(classOf[JdbcContext[_, _]]))

  override type PrepareRow = PreparedStatement
  override type ResultRow = ResultSet

  override type RunQueryResult[T] = List[T]
  override type RunQuerySingleResult[T] = T
  override type RunActionResult = Long
  override type RunActionReturningResult[T] = T
  override type RunBatchActionResult = List[Long]
  override type RunBatchActionReturningResult[T] = List[T]

  protected val currentConnection = new DynamicVariable[Option[Connection]](None)

  protected def withConnection[T](f: Connection => T) =
    currentConnection.value.map(f).getOrElse {
      val conn = dataSource.getConnection
      try f(conn)
      finally conn.close()
    }

  def close() = dataSource.close()

  def probe(sql: String) =
    Try {
      withConnection(_.createStatement.execute(sql))
    }

  def transaction[T](f: => T) =
    currentConnection.value match {
      case Some(_) => f // already in transaction
      case None =>
        withConnection { conn =>
          currentConnection.withValue(Some(conn)) {
            val wasAutoCommit = conn.getAutoCommit
            conn.setAutoCommit(false)
            try {
              val res = f
              conn.commit()
              res
            } catch {
              case NonFatal(e) =>
                conn.rollback()
                throw e
            } finally
              conn.setAutoCommit(wasAutoCommit)
          }
        }
    }

  def executeQuery[T](sql: String, prepare: PreparedStatement => PreparedStatement = identity, extractor: ResultSet => T = identity[ResultSet] _): List[T] =
    withConnection { conn =>
      logger.info(sql)
      val ps = prepare(conn.prepareStatement(sql))
      val rs = ps.executeQuery()
      extractResult(rs, extractor)
    }

  def executeQuerySingle[T](sql: String, prepare: PreparedStatement => PreparedStatement = identity, extractor: ResultSet => T = identity[ResultSet] _): T =
    handleSingleResult(executeQuery(sql, prepare, extractor))

  def executeAction[T](sql: String, prepare: PreparedStatement => PreparedStatement = identity): Long =
    withConnection { conn =>
      logger.info(sql)
      prepare(conn.prepareStatement(sql)).executeUpdate().toLong
    }

  def executeActionReturning[O](sql: String, prepare: PreparedStatement => PreparedStatement = identity, extractor: ResultSet => O, returningColumn: String): O =
    withConnection { conn =>
      logger.info(sql)
      val ps = prepare(conn.prepareStatement(sql, Array(returningColumn)))
      ps.executeUpdate()
      handleSingleResult(extractResult(ps.getGeneratedKeys, extractor))
    }

  def executeBatchAction(groups: List[BatchGroup]): List[Long] =
    withConnection { conn =>
      groups.flatMap {
        case BatchGroup(sql, prepare) =>
          logger.info(sql)
          val ps = conn.prepareStatement(sql)
          prepare.foreach { f =>
            f(ps)
            ps.addBatch()
          }
          ps.executeBatch().map(_.toLong)
      }
    }

  def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: ResultSet => T): List[T] =
    withConnection { conn =>
      groups.flatMap {
        case BatchGroupReturning(sql, column, prepare) =>
          logger.info(sql)
          val ps = conn.prepareStatement(sql, Array(column))
          prepare.foreach { f =>
            f(ps)
            ps.addBatch()
          }
          ps.executeBatch()
          extractResult(ps.getGeneratedKeys, extractor)
      }
    }

  @tailrec
  private def extractResult[T](rs: ResultSet, extractor: ResultSet => T, acc: List[T] = List()): List[T] =
    if (rs.next)
      extractResult(rs, extractor, extractor(rs) :: acc)
    else
      acc.reverse
}
