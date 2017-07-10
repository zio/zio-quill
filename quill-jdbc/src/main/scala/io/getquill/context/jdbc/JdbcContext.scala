package io.getquill.context.jdbc

import java.io.Closeable
import java.sql.{ Connection, JDBCType, PreparedStatement, ResultSet }
import javax.sql.DataSource

import io.getquill.context.sql.SqlContext
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.NamingStrategy
import io.getquill.util.ContextLogger

import scala.annotation.tailrec
import scala.util.{ DynamicVariable, Try }
import scala.util.control.NonFatal

abstract class JdbcContext[Dialect <: SqlIdiom, Naming <: NamingStrategy](dataSource: DataSource with Closeable)
  extends SqlContext[Dialect, Naming]
  with Encoders
  with Decoders {

  private val logger = ContextLogger(classOf[JdbcContext[_, _]])

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

  def executeQuery[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): List[T] =
    withConnection { conn =>
      val (params, ps) = prepare(conn.prepareStatement(sql))
      logger.logQuery(sql, params)
      val rs = ps.executeQuery()
      extractResult(rs, extractor)
    }

  def executeQuerySingle[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): T =
    handleSingleResult(executeQuery(sql, prepare, extractor))

  def executeAction[T](sql: String, prepare: Prepare = identityPrepare): Long =
    withConnection { conn =>
      val (params, ps) = prepare(conn.prepareStatement(sql))
      logger.logQuery(sql, params)
      ps.executeUpdate().toLong
    }

  def executeActionReturning[O](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[O], returningColumn: String): O =
    withConnection { conn =>
      val (params, ps) = prepare(conn.prepareStatement(sql, Array(returningColumn)))
      logger.logQuery(sql, params)
      ps.executeUpdate()
      handleSingleResult(extractResult(ps.getGeneratedKeys, extractor))
    }

  def executeBatchAction(groups: List[BatchGroup]): List[Long] =
    withConnection { conn =>
      groups.flatMap {
        case BatchGroup(sql, prepare) =>
          val ps = conn.prepareStatement(sql)
          logger.underlying.debug("Batch: {}", sql)
          prepare.foreach { f =>
            val (params, _) = f(ps)
            logger.logBatchItem(sql, params)
            ps.addBatch()
          }
          ps.executeBatch().map(_.toLong)
      }
    }

  def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: Extractor[T]): List[T] =
    withConnection { conn =>
      groups.flatMap {
        case BatchGroupReturning(sql, column, prepare) =>
          val ps = conn.prepareStatement(sql, Array(column))
          logger.underlying.debug("Batch: {}", sql)
          prepare.foreach { f =>
            val (params, _) = f(ps)
            logger.logBatchItem(sql, params)
            ps.addBatch()
          }
          ps.executeBatch()
          extractResult(ps.getGeneratedKeys, extractor)
      }
    }

  /**
   * Parses instances of java.sql.Types to string form so it can be used in creation of sql arrays.
   * Some databases does not support each of generic types, hence it's welcome to override this method
   * and provide alternatives to non-existent types.
   *
   * @param intType one of java.sql.Types
   * @return JDBC type in string form
   */
  def parseJdbcType(intType: Int): String = JDBCType.valueOf(intType).getName

  @tailrec
  private def extractResult[T](rs: ResultSet, extractor: Extractor[T], acc: List[T] = List()): List[T] =
    if (rs.next)
      extractResult(rs, extractor, extractor(rs) :: acc)
    else
      acc.reverse
}
