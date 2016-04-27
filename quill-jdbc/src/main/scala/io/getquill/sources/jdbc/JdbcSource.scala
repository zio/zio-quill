package io.getquill.sources.jdbc

import java.sql.{ Connection, PreparedStatement, ResultSet }

import scala.util.DynamicVariable

import io.getquill.naming.NamingStrategy
import io.getquill.sources.sql.{ SqlBindedStatementBuilder, SqlSource }
import scala.util.Try

import io.getquill.sources.sql.idiom.SqlIdiom
import scala.util.control.NonFatal
import scala.annotation.tailrec

import io.getquill.JdbcSourceConfig
import io.getquill.sources.BindedStatementBuilder
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

class JdbcSource[D <: SqlIdiom, N <: NamingStrategy](config: JdbcSourceConfig[D, N])
  extends SqlSource[D, N, ResultSet, BindedStatementBuilder[PreparedStatement]]
  with JdbcEncoders
  with JdbcDecoders {

  protected val logger: Logger =
    Logger(LoggerFactory.getLogger(classOf[JdbcSource[_, _]]))

  type QueryResult[T] = List[T]
  type ActionResult[T] = Long
  type BatchedActionResult[T] = List[Long]

  class ActionApply[T](f: List[T] => List[Long]) extends Function1[List[T], List[Long]] {
    def apply(params: List[T]) = f(params)
    def apply(param: T) = f(List(param)).head
  }

  private val dataSource = config.dataSource

  override def close = dataSource.close

  private val currentConnection = new DynamicVariable[Option[Connection]](None)

  protected def withConnection[T](f: Connection => T) =
    currentConnection.value.map(f).getOrElse {
      val conn = dataSource.getConnection
      try f(conn)
      finally conn.close
    }

  def probe(sql: String) =
    withConnection { conn =>
      Try {
        conn.createStatement.execute(sql)
      }
    }

  def transaction[T](f: => T) =
    withConnection { conn =>
      currentConnection.withValue(Some(conn)) {
        conn.setAutoCommit(false)
        try {
          val res = f
          conn.commit
          res
        } catch {
          case NonFatal(e) =>
            conn.rollback
            throw e
        } finally {
          conn.setAutoCommit(true)
        }
      }
    }

  def execute(sql: String, bind: BindedStatementBuilder[PreparedStatement] => BindedStatementBuilder[PreparedStatement], generated: Option[String] = None): Long = {
    logger.info(sql)
    val (expanded, setValues) = bind(new SqlBindedStatementBuilder[PreparedStatement]).build(sql)
    logger.info(expanded)
    withConnection { conn =>
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
  }

  def executeBatch[T](sql: String, bindParams: T => BindedStatementBuilder[PreparedStatement] => BindedStatementBuilder[PreparedStatement],
                      generated: Option[String] = None): ActionApply[T] = {
    val func = { (values: List[T]) =>
      val groups = values.map(bindParams(_)(new SqlBindedStatementBuilder[PreparedStatement]).build(sql)).groupBy(_._1)
      (for ((sql, setValues) <- groups.toList) yield {
        logger.info(sql)
        withConnection { conn =>
          val ps = generated.fold(conn.prepareStatement(sql))(c => conn.prepareStatement(sql, Array(c)))
          for ((_, set) <- setValues) {
            set(ps)
            ps.addBatch
          }
          val updateCount = ps.executeBatch.toList.map(_.toLong)
          generated.fold(updateCount)(_ => extractResult(ps.getGeneratedKeys, _.getLong(1)))
        }
      }).flatten
    }
    new ActionApply(func)
  }

  def query[T](sql: String, bind: BindedStatementBuilder[PreparedStatement] => BindedStatementBuilder[PreparedStatement], extractor: ResultSet => T): List[T] = {
    val (expanded, setValues) = bind(new SqlBindedStatementBuilder[PreparedStatement]).build(sql)
    logger.info(expanded)
    withConnection { conn =>
      val ps = setValues(conn.prepareStatement(expanded))
      val rs = ps.executeQuery
      extractResult(rs, extractor)
    }
  }

  @tailrec
  private def extractResult[T](rs: ResultSet, extractor: ResultSet => T, acc: List[T] = List()): List[T] =
    if (rs.next)
      extractResult(rs, extractor, acc :+ extractor(rs))
    else
      acc

}
