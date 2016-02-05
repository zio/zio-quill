package io.getquill.sources.jdbc

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import scala.util.DynamicVariable
import com.typesafe.scalalogging.StrictLogging
import io.getquill.naming.NamingStrategy
import io.getquill.sources.sql.SqlSource
import scala.util.Try
import io.getquill.sources.sql.idiom.SqlIdiom
import scala.util.control.NonFatal
import scala.annotation.tailrec
import io.getquill.JdbcSourceConfig
import io.getquill.sources.BindedStatementBuilder

class JdbcSource[D <: SqlIdiom, N <: NamingStrategy](config: JdbcSourceConfig[D, N])
  extends SqlSource[D, N, ResultSet, BindedStatementBuilder[PreparedStatement]]
  with JdbcEncoders
  with JdbcDecoders
  with StrictLogging {

  type QueryResult[T] = List[T]
  type ActionResult[T] = Int
  type BatchedActionResult[T] = List[Int]

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

  def execute(sql: String): Int = {
    logger.info(sql)
    withConnection {
      _.prepareStatement(sql).executeUpdate
    }
  }

  def execute[T](sql: String, bindParams: T => BindedStatementBuilder[PreparedStatement] => BindedStatementBuilder[PreparedStatement]): List[T] => List[Int] = {
    (values: List[T]) =>
      val groups = values.map(bindParams(_)(new BindedStatementBuilder[PreparedStatement]).build(sql)).groupBy(_._1)
      (for ((sql, setValues) <- groups.toList) yield {
        logger.info(sql)
        withConnection { conn =>
          val ps = conn.prepareStatement(sql)
          for ((_, set) <- setValues) {
            set(ps)
            ps.addBatch
          }
          ps.executeBatch.toList
        }
      }).flatten
  }

  def query[T](sql: String, bind: BindedStatementBuilder[PreparedStatement] => BindedStatementBuilder[PreparedStatement], extractor: ResultSet => T): List[T] = {
    val (expanded, setValues) = bind(new BindedStatementBuilder[PreparedStatement]).build(sql)
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
