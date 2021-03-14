package io.getquill.context.jdbc

import java.sql._

import io.getquill._
import io.getquill.ReturnAction._
import io.getquill.context.sql.SqlContext
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.context.{ Context, ContextEffect, StagedPrepare, PrepareContext }
import io.getquill.util.ContextLogger

trait JdbcContextBase[Dialect <: SqlIdiom, Naming <: NamingStrategy] extends JdbcContextSimplified[Dialect, Naming]
  with StagedPrepare {
  def constructPrepareQuery(f: Connection => Result[PreparedStatement]): Connection => Result[PreparedStatement] = f
  def constructPrepareAction(f: Connection => Result[PreparedStatement]): Connection => Result[PreparedStatement] = f
  def constructPrepareBatchAction(f: Connection => Result[List[PreparedStatement]]): Connection => Result[List[PreparedStatement]] = f
}

trait JdbcContextSimplified[Dialect <: SqlIdiom, Naming <: NamingStrategy]
  extends JdbcRunContext[Dialect, Naming] with PrepareContext {

  import effect._
  def constructPrepareQuery(f: Connection => Result[PreparedStatement]): PrepareQueryResult
  def constructPrepareAction(f: Connection => Result[PreparedStatement]): PrepareActionResult
  def constructPrepareBatchAction(f: Connection => Result[List[PreparedStatement]]): PrepareBatchActionResult

  def prepareQuery[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): PrepareQueryResult =
    constructPrepareQuery(prepareSingle(sql, prepare))

  def prepareAction(sql: String, prepare: Prepare = identityPrepare): PrepareActionResult =
    constructPrepareAction(prepareSingle(sql, prepare))

  def prepareSingle(sql: String, prepare: Prepare = identityPrepare): Connection => Result[PreparedStatement] =
    (conn: Connection) => wrap {
      val (params, ps) = prepare(conn.prepareStatement(sql))
      logger.logQuery(sql, params)
      ps
    }

  def prepareBatchAction(groups: List[BatchGroup]): PrepareBatchActionResult =
    constructPrepareBatchAction {
      (session: Connection) =>
        seq {
          val batches = groups.flatMap {
            case BatchGroup(sql, prepares) =>
              prepares.map(sql -> _)
          }
          batches.map {
            case (sql, prepare) =>
              val prepareSql = prepareSingle(sql, prepare)
              prepareSql(session)
          }
        }
    }

}

trait JdbcRunContext[Dialect <: SqlIdiom, Naming <: NamingStrategy]
  extends Context[Dialect, Naming]
  with SqlContext[Dialect, Naming]
  with Encoders
  with Decoders {
  private[getquill] val logger = ContextLogger(classOf[JdbcContext[_, _]])

  override type PrepareRow = PreparedStatement
  override type ResultRow = ResultSet
  override type Session = Connection

  protected val effect: ContextEffect[Result]
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

  def executeActionReturning[O](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[O], returningBehavior: ReturnAction): Result[O] =
    withConnectionWrapped { conn =>
      val (params, ps) = prepare(prepareWithReturning(sql, conn, returningBehavior))
      logger.logQuery(sql, params)
      ps.executeUpdate()
      handleSingleResult(extractResult(ps.getGeneratedKeys, extractor))
    }

  protected def prepareWithReturning(sql: String, conn: Connection, returningBehavior: ReturnAction) =
    returningBehavior match {
      case ReturnRecord           => conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
      case ReturnColumns(columns) => conn.prepareStatement(sql, columns.toArray)
      case ReturnNothing          => conn.prepareStatement(sql)
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
        case BatchGroupReturning(sql, returningBehavior, prepare) =>
          val ps = prepareWithReturning(sql, conn, returningBehavior)
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

  private[getquill] final def extractResult[T](rs: ResultSet, extractor: Extractor[T]): List[T] =
    ResultSetExtractor(rs, extractor)
}
