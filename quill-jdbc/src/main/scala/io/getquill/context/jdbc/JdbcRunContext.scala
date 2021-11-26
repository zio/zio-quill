package io.getquill.context.jdbc

import io.getquill.{ NamingStrategy, ReturnAction }
import io.getquill.ReturnAction.{ ReturnColumns, ReturnNothing, ReturnRecord }
import io.getquill.context.{ Context, ContextEffect, ExecutionInfo }
import io.getquill.context.sql.SqlContext
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.util.ContextLogger

import java.sql.{ Connection, JDBCType, PreparedStatement, ResultSet, Statement }
import java.util.TimeZone

trait JdbcComposition[Dialect <: SqlIdiom, Naming <: NamingStrategy] extends Context[Dialect, Naming]
  with SqlContext[Dialect, Naming]
  with Encoders
  with Decoders {

  type PrepareRow = PreparedStatement
  type ResultRow = ResultSet
  type Session = Connection
  type DatasourceContext = Unit

  protected val dateTimeZone = TimeZone.getDefault

  /**
   * Parses instances of java.sql.Types to string form so it can be used in creation of sql arrays.
   * Some databases does not support each of generic types, hence it's welcome to override this method
   * and provide alternatives to non-existent types.
   *
   * @param intType one of java.sql.Types
   * @return JDBC type in string form
   */
  def parseJdbcType(intType: Int): String = JDBCType.valueOf(intType).getName
}

trait JdbcRunContext[Dialect <: SqlIdiom, Naming <: NamingStrategy] extends JdbcComposition[Dialect, Naming] {
  private[getquill] val logger = ContextLogger(classOf[JdbcContext[_, _]])

  protected val effect: ContextEffect[Result]
  import effect._

  protected def withConnection[T](f: Connection => Result[T]): Result[T]
  protected def withConnectionWrapped[T](f: Connection => T): Result[T] =
    withConnection(conn => wrap(f(conn)))

  def executeAction[T](sql: String, prepare: Prepare = identityPrepare)(info: ExecutionInfo, dc: DatasourceContext): Result[Long] =
    withConnectionWrapped { conn =>
      val (params, ps) = prepare(conn.prepareStatement(sql), conn)
      logger.logQuery(sql, params)
      ps.executeUpdate().toLong
    }

  def executeQuery[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(info: ExecutionInfo, dc: DatasourceContext): Result[List[T]] =
    withConnectionWrapped { conn =>
      val (params, ps) = prepare(conn.prepareStatement(sql), conn)
      logger.logQuery(sql, params)
      val rs = ps.executeQuery()
      extractResult(rs, conn, extractor)
    }

  def executeQuerySingle[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(info: ExecutionInfo, dc: DatasourceContext): Result[T] =
    handleSingleWrappedResult(executeQuery(sql, prepare, extractor)(info, dc))

  def executeActionReturning[O](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[O], returningBehavior: ReturnAction)(info: ExecutionInfo, dc: DatasourceContext): Result[O] =
    withConnectionWrapped { conn =>
      val (params, ps) = prepare(prepareWithReturning(sql, conn, returningBehavior), conn)
      logger.logQuery(sql, params)
      ps.executeUpdate()
      handleSingleResult(extractResult(ps.getGeneratedKeys, conn, extractor))
    }

  protected def prepareWithReturning(sql: String, conn: Connection, returningBehavior: ReturnAction) =
    returningBehavior match {
      case ReturnRecord           => conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
      case ReturnColumns(columns) => conn.prepareStatement(sql, columns.toArray)
      case ReturnNothing          => conn.prepareStatement(sql)
    }

  def executeBatchAction(groups: List[BatchGroup])(info: ExecutionInfo, dc: DatasourceContext): Result[List[Long]] =
    withConnectionWrapped { conn =>
      groups.flatMap {
        case BatchGroup(sql, prepare) =>
          val ps = conn.prepareStatement(sql)
          logger.underlying.debug("Batch: {}", sql)
          prepare.foreach { f =>
            val (params, _) = f(ps, conn)
            logger.logBatchItem(sql, params)
            ps.addBatch()
          }
          ps.executeBatch().map(_.toLong)
      }
    }

  def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: Extractor[T])(info: ExecutionInfo, dc: DatasourceContext): Result[List[T]] =
    withConnectionWrapped { conn =>
      groups.flatMap {
        case BatchGroupReturning(sql, returningBehavior, prepare) =>
          val ps = prepareWithReturning(sql, conn, returningBehavior)
          logger.underlying.debug("Batch: {}", sql)
          prepare.foreach { f =>
            val (params, _) = f(ps, conn)
            logger.logBatchItem(sql, params)
            ps.addBatch()
          }
          ps.executeBatch()
          extractResult(ps.getGeneratedKeys, conn, extractor)
      }
    }

  protected def handleSingleWrappedResult[T](list: Result[List[T]]): Result[T] =
    push(list)(handleSingleResult(_))

  private[getquill] final def extractResult[T](rs: ResultSet, conn: Connection, extractor: Extractor[T]): List[T] =
    ResultSetExtractor(rs, conn, extractor)
}
