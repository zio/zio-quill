package io.getquill.context.streaming

import java.sql.{ Connection, JDBCType, PreparedStatement, ResultSet }
import scala.annotation.tailrec
import cats.syntax.functor._
import cats.effect.Sync
import io.getquill.NamingStrategy
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.context.sql.SqlContext
import io.getquill.context.Context
import io.getquill.util.ContextLogger

abstract class StreamingContext[F[_], Dialect <: SqlIdiom, Naming <: NamingStrategy](implicit F: Sync[F])
  extends Context[Dialect, Naming]
  with SqlContext[Dialect, Naming]
  with Encoders[F]
  with Decoders[F] {

  protected val logger = ContextLogger(getClass)

  override type PrepareRow = PreparedStatement
  override type ResultRow = ResultSet

  override type Result[T] = F[T]
  override type RunQueryResult[T] = List[T]
  override type RunQuerySingleResult[T] = T
  override type RunActionResult = Long
  override type RunActionReturningResult[T] = T
  override type RunBatchActionResult = List[Long]
  override type RunBatchActionReturningResult[T] = List[T]

  def transaction[A](f: F[A]): F[A]

  protected def withConnection[T](f: Connection => F[T]): F[T]

  def executeQuery[T](
    sql:       String,
    prepare:   Prepare      = identityPrepare,
    extractor: Extractor[T] = identityExtractor
  ): F[List[T]] = withConnection { connection =>
    F.delay {
      val (params, ps) = prepare(connection.prepareStatement(sql))
      logger.logQuery(sql, params)
      val rs = ps.executeQuery()
      extractResult(rs, extractor)
    }
  }

  def executeQuerySingle[T](
    sql:       String,
    prepare:   Prepare      = identityPrepare,
    extractor: Extractor[T] = identityExtractor
  ): F[T] =
    executeQuery(sql, prepare, extractor).map(handleSingleResult)

  def executeAction[T](sql: String, prepare: Prepare = identityPrepare): F[Long] = withConnection { connection =>
    F.delay {
      val (params, ps) = prepare(connection.prepareStatement(sql))
      logger.logQuery(sql, params)
      ps.executeUpdate().toLong
    }
  }

  def executeActionReturning[O](
    sql:             String,
    prepare:         Prepare      = identityPrepare,
    extractor:       Extractor[O],
    returningColumn: String
  ): F[O] = withConnection { connection =>
    F.delay {
      val (params, ps) = prepare(connection.prepareStatement(sql, Array(returningColumn)))
      logger.logQuery(sql, params)
      ps.executeUpdate()
      handleSingleResult(extractResult(ps.getGeneratedKeys, extractor))
    }
  }

  def executeBatchAction(groups: List[BatchGroup]): F[List[Long]] = withConnection { connection =>
    F.delay {
      groups.flatMap {
        case BatchGroup(sql, prepare) =>
          val ps = connection.prepareStatement(sql)
          logger.underlying.debug("Batch: {}", sql)
          prepare.foreach { f =>
            val (params, _) = f(ps)
            logger.logBatchItem(sql, params)
            ps.addBatch()
          }
          ps.executeBatch().map(_.toLong)
      }
    }
  }

  def executeBatchActionReturning[T](
    groups:    List[BatchGroupReturning],
    extractor: Extractor[T]              = identityExtractor
  ): F[List[T]] = withConnection {
    connection =>
      F.delay {
        groups.flatMap {
          case BatchGroupReturning(sql, column, prepare) =>
            val ps = connection.prepareStatement(sql, Array(column))
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
