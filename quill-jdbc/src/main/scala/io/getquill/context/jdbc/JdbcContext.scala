package io.getquill.context.jdbc

import com.typesafe.config.Config
import com.typesafe.scalalogging.Logger

import java.io.Closeable
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

import org.slf4j.LoggerFactory

import scala.annotation.tailrec
import scala.util.Try
import scala.util.control.NonFatal

import io.getquill.context.BindedStatementBuilder
import io.getquill.context.sql.SqlBindedStatementBuilder
import io.getquill.context.sql.SqlContext
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.naming.NamingStrategy
import io.getquill.util.LoadConfig
import javax.sql.DataSource
import scala.util.DynamicVariable

class JdbcContext[D <: SqlIdiom, N <: NamingStrategy](dataSource: DataSource with Closeable)
  extends SqlContext[D, N, ResultSet, BindedStatementBuilder[PreparedStatement]]
  with JdbcEncoders
  with JdbcDecoders {

  def this(config: Config) = this(CreateDataSource(config))
  def this(configPrefix: String) = this(LoadConfig(configPrefix))

  protected val logger: Logger =
    Logger(LoggerFactory.getLogger(classOf[JdbcContext[_, _]]))

  type QueryResult[T] = List[T]
  type SingleQueryResult[T] = T
  type ActionResult[T] = Long
  type BatchedActionResult[T] = List[Long]

  class ActionApply[T](f: List[T] => List[Long]) extends Function1[List[T], List[Long]] {
    def apply(params: List[T]) = f(params)

    def apply(param: T) = f(List(param)).head
  }

  private val currentConnection = new DynamicVariable[Option[Connection]](None)

  protected def withConnection[T](f: Connection => T) =
    currentConnection.value.map(f).getOrElse {
      val conn = dataSource.getConnection
      try f(conn)
      finally conn.close
    }

  def close = dataSource.close()

  def probe(sql: String) =
    Try {
      withConnection(_.createStatement.execute(sql))
    }

  def transaction[T](f: => T) =
    withConnection { conn =>
      currentConnection.withValue(Some(conn)) {
        val wasAutoCommit = conn.getAutoCommit
        conn.setAutoCommit(false)
        try {
          val res = f
          conn.commit
          res
        } catch {
          case NonFatal(e) =>
            conn.rollback
            throw e
        } finally
          conn.setAutoCommit(wasAutoCommit)
      }
    }

  def executeAction(sql: String, bind: BindedStatementBuilder[PreparedStatement] => BindedStatementBuilder[PreparedStatement] = identity, generated: Option[String] = None): Long =
    withConnection { conn =>
      logger.info(sql)
      val (expanded, setValues) = bind(new SqlBindedStatementBuilder[PreparedStatement]).build(sql)
      logger.info(expanded)
      generated match {
        case None =>
          val ps = setValues(conn.prepareStatement(expanded))
          ps.executeUpdate.toLong
        case Some(column) =>
          val ps = setValues(conn.prepareStatement(expanded, Array(column)))
          val rs = ps.executeUpdate
          extractResult(ps.getGeneratedKeys, _.getLong(1)).head
      }
    }

  def executeActionBatch[T](sql: String, bindParams: T => BindedStatementBuilder[PreparedStatement] => BindedStatementBuilder[PreparedStatement] = (_: T) => identity[BindedStatementBuilder[PreparedStatement]] _,
                            generated: Option[String] = None): ActionApply[T] = {
    val func = { (values: List[T]) =>
      withConnection { conn =>
        val groups = values.map(bindParams(_)(new SqlBindedStatementBuilder[PreparedStatement]).build(sql)).groupBy(_._1)
        (for ((sql, setValues) <- groups.toList) yield {
          logger.info(sql)
          val ps = generated.fold(conn.prepareStatement(sql))(c => conn.prepareStatement(sql, Array(c)))
          for ((_, set) <- setValues) {
            set(ps)
            ps.addBatch
          }
          val updateCount = ps.executeBatch.toList.map(_.toLong)
          generated.fold(updateCount)(_ => extractResult(ps.getGeneratedKeys, _.getLong(1)))
        }).flatten
      }
    }
    new ActionApply(func)
  }

  def executeQuery[T](sql: String, extractor: ResultSet => T = identity[ResultSet] _, bind: BindedStatementBuilder[PreparedStatement] => BindedStatementBuilder[PreparedStatement] = identity): List[T] =
    withConnection { conn =>
      val (expanded, setValues) = bind(new SqlBindedStatementBuilder[PreparedStatement]).build(sql)
      logger.info(expanded)
      val ps = setValues(conn.prepareStatement(expanded))
      val rs = ps.executeQuery
      extractResult(rs, extractor)
    }

  def executeQuerySingle[T](sql: String, extractor: ResultSet => T = identity[ResultSet] _, bind: BindedStatementBuilder[PreparedStatement] => BindedStatementBuilder[PreparedStatement] = identity): T =
    handleSingleResult(executeQuery(sql, extractor, bind))

  @tailrec
  private def extractResult[T](rs: ResultSet, extractor: ResultSet => T, acc: List[T] = List()): List[T] =
    if (rs.next)
      extractResult(rs, extractor, acc :+ extractor(rs))
    else
      acc

}
