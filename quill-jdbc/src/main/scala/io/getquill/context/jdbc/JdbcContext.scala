package io.getquill.context.jdbc

import java.io.Closeable
import java.sql.{ Connection, PreparedStatement }

import javax.sql.DataSource
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.{ NamingStrategy, ReturnAction }
import io.getquill.context.{ ContextEffect, TranslateContext }

import scala.util.{ DynamicVariable, Try }
import scala.util.control.NonFatal
import io.getquill.monad.SyncIOMonad

abstract class JdbcContext[Dialect <: SqlIdiom, Naming <: NamingStrategy]
  extends JdbcContextBase[Dialect, Naming]
  with TranslateContext
  with SyncIOMonad {

  val dataSource: DataSource with Closeable

  override type Result[T] = T
  override type RunQueryResult[T] = List[T]
  override type RunQuerySingleResult[T] = T
  override type RunActionResult = Long
  override type RunActionReturningResult[T] = T
  override type RunBatchActionResult = List[Long]
  override type RunBatchActionReturningResult[T] = List[T]

  override protected val effect: ContextEffect[Result] = new ContextEffect[Result] {
    override def wrap[T](t: => T): T = t
    override def push[A, B](result: A)(f: A => B): B = f(result)
    override def seq[A](list: List[A]): List[A] = list
  }

  // Need explicit return-type annotations due to scala/bug#8356. Otherwise macro system will not understand Result[Long]=Long etc...
  override def executeAction[T](sql: String, prepare: Prepare = identityPrepare): Long =
    super.executeAction(sql, prepare)
  override def executeQuery[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): List[T] =
    super.executeQuery(sql, prepare, extractor)
  override def executeQuerySingle[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): T =
    super.executeQuerySingle(sql, prepare, extractor)
  override def executeActionReturning[O](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[O], returningBehavior: ReturnAction): O =
    super.executeActionReturning(sql, prepare, extractor, returningBehavior)
  override def executeBatchAction(groups: List[BatchGroup]): List[Long] =
    super.executeBatchAction(groups)
  override def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: Extractor[T]): List[T] =
    super.executeBatchActionReturning(groups, extractor)
  override def prepareQuery[T](sql: String, prepare: Prepare, extractor: Extractor[T] = identityExtractor): Connection => PreparedStatement =
    super.prepareQuery(sql, prepare, extractor)
  override def prepareAction(sql: String, prepare: Prepare): Connection => PreparedStatement =
    super.prepareAction(sql, prepare)
  override def prepareBatchAction(groups: List[BatchGroup]): Connection => List[PreparedStatement] =
    super.prepareBatchAction(groups)

  protected val currentConnection = new DynamicVariable[Option[Connection]](None)

  protected def withConnection[T](f: Connection => Result[T]) =
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

  override def performIO[T](io: IO[T, _], transactional: Boolean = false): Result[T] =
    transactional match {
      case false => super.performIO(io)
      case true  => transaction(super.performIO(io))
    }

  override private[getquill] def prepareParams(statement: String, prepare: Prepare): Seq[String] = {
    withConnectionWrapped { conn =>
      prepare(conn.prepareStatement(statement))._1.reverse.map(prepareParam)
    }
  }
}
