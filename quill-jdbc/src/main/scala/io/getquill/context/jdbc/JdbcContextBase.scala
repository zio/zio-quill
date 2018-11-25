package io.getquill.context.jdbc

import java.sql.{ Connection, JDBCType, PreparedStatement, ResultSet }

import io.getquill.NamingStrategy
import io.getquill.context.{ Context, ContextEffect }
import io.getquill.context.sql.SqlContext
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.util.ContextLogger

import scala.annotation.tailrec

trait JdbcContextBase[Dialect <: SqlIdiom, Naming <: NamingStrategy]
  extends Context[Dialect, Naming]
  with SqlContext[Dialect, Naming]
  with Encoders
  with Decoders {
  private[getquill] val logger = ContextLogger(classOf[JdbcContext[_, _]])

  override type PrepareRow = PreparedStatement
  override type ResultRow = ResultSet

  private[getquill] val effect: ContextEffect[Result]
  import effect._

  protected def withConnection[T](f: Connection => Result[T]): Result[T]
  protected def withConnectionWrapped[T](f: Connection => T): Result[T] =
    withConnection(conn => wrap(f(conn)))

  def executeAction[T](sql: String, prepare: Prepare = identityPrepare): Result[Long] =
    withConnectionWrapped { conn =>
      val (params, ps) = prepare(conn.prepareStatement(sql))
      logger.logQuery(sql, params)
      ps.executeUpdate().toLong
    }

  def executeQuery[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): Result[List[T]] =
    withConnectionWrapped { conn =>
      val (params, ps) = prepare(conn.prepareStatement(sql))
      logger.logQuery(sql, params)
      val rs = ps.executeQuery()
      extractResult(rs, extractor)
    }

  def executeQuerySingle[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): Result[T] =
    handleSingleWrappedResult(executeQuery(sql, prepare, extractor))

  def executeActionReturning[O](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[O], returningColumn: String): Result[O] =
    withConnectionWrapped { conn =>
      val (params, ps) = prepare(conn.prepareStatement(sql, Array(returningColumn)))
      logger.logQuery(sql, params)
      ps.executeUpdate()
      handleSingleResult(extractResult(ps.getGeneratedKeys, extractor))
    }

  def executeBatchAction(groups: List[BatchGroup]): Result[List[Long]] =
    withConnectionWrapped { conn =>
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

  def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: Extractor[T]): Result[List[T]] =
    withConnectionWrapped { conn =>
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

  protected def handleSingleWrappedResult[T](list: Result[List[T]]): Result[T] =
    push(list)(handleSingleResult(_))

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
  private[getquill] final def extractResult[T](rs: ResultSet, extractor: Extractor[T], acc: List[T] = List()): List[T] =
    if (rs.next)
      extractResult(rs, extractor, extractor(rs) :: acc)
    else
      acc.reverse
}
