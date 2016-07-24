package io.getquill

import com.typesafe.config.Config
import com.typesafe.scalalogging.Logger
import java.io.Closeable
import java.sql.{ Connection, PreparedStatement, ResultSet }

import org.slf4j.LoggerFactory

import scala.annotation.tailrec
import scala.util.Try
import scala.util.control.NonFatal
import io.getquill.context.BindedStatementBuilder
import io.getquill.context.sql.SqlBindedStatementBuilder
import io.getquill.context.sql.SqlContext
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.util.LoadConfig
import javax.sql.DataSource

import scala.util.DynamicVariable
import io.getquill.context.jdbc.JdbcDecoders
import io.getquill.context.jdbc.JdbcEncoders

import scala.reflect.runtime.universe._

//import io.getquill.context.jdbc.ActionApply

class JdbcContext[D <: SqlIdiom, N <: NamingStrategy](dataSource: DataSource with Closeable)
  extends SqlContext[D, N, ResultSet, BindedStatementBuilder[PreparedStatement]]
  with JdbcEncoders
  with JdbcDecoders {

  def this(config: JdbcContextConfig) = this(config.dataSource)
  def this(config: Config) = this(JdbcContextConfig(config))
  def this(configPrefix: String) = this(LoadConfig(configPrefix))

  private val logger: Logger =
    Logger(LoggerFactory.getLogger(classOf[JdbcContext[_, _]]))

  type QueryResult[T] = List[T]
  type SingleQueryResult[T] = T
  type ActionResult[T, O] = O
  type BatchedActionResult[T, O] = List[O]

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
    currentConnection.value match {
      case Some(_) => f // already in transaction
      case None =>
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
    }

  def executeAction[O](
    sql:                String,
    bind:               BindedStatementBuilder[PreparedStatement] => BindedStatementBuilder[PreparedStatement] = identity,
    returning:          Option[String]                                                                         = None,
    returningExtractor: ResultSet => O                                                                         = identity[ResultSet] _
  ): O =
    withConnection { conn =>
      logger.info(sql)
      val (expanded, setValues) = bind(new SqlBindedStatementBuilder[PreparedStatement]).build(sql)
      logger.info(expanded)

      val ps = setValues(returning.fold(conn.prepareStatement(sql))(c => conn.prepareStatement(sql, Array(c))))
      val updateCount = ps.executeUpdate.toLong
      returning match {
        case None    => updateCount.asInstanceOf[O] // TODO: get rid of this ugly asInstanceOf
        case Some(_) => extractResult(ps.getGeneratedKeys, returningExtractor).head
      }
    }

  def executeActionBatch[T, O](
    sql:                String,
    bindParams:         T => BindedStatementBuilder[PreparedStatement] => BindedStatementBuilder[PreparedStatement] = (_: T) => identity[BindedStatementBuilder[PreparedStatement]] _,
    returning:          Option[String]                                                                              = None,
    returningExtractor: ResultSet => O                                                                              = identity[ResultSet] _
  ): List[T] => List[O] = {
    val func = { (values: List[T]) =>
      withConnection { conn =>
        val groups = values.map(bindParams(_)(new SqlBindedStatementBuilder[PreparedStatement]).build(sql)).groupBy(_._1)
        (for ((sql, setValues) <- groups.toList) yield {
          logger.info(sql)
          val ps = returning.fold(conn.prepareStatement(sql))(c => conn.prepareStatement(sql, Array(c)))
          for ((_, set) <- setValues) {
            set(ps)
            ps.addBatch
          }
          val updateCount = ps.executeBatch.toList.map(_.toLong)
          returning match {
            case None    => updateCount.asInstanceOf[List[O]]
            case Some(_) => extractResult(ps.getGeneratedKeys, returningExtractor)
          }
        }).flatten
      }
    }
    func
  }

  def executeQuery[T](sql: String, extractor: ResultSet => T = identity[ResultSet] _,
                      bind: BindedStatementBuilder[PreparedStatement] => BindedStatementBuilder[PreparedStatement] = identity): List[T] =
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
