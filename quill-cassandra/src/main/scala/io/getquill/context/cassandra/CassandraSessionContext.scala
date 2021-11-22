package io.getquill.context.cassandra

import com.datastax.oss.driver.api.core.cql.{ BoundStatement, Row }
import io.getquill.NamingStrategy
import io.getquill.context.{ CassandraSession, ExecutionInfo, StandardContext, UdtValueLookup }
import io.getquill.context.cassandra.encoding.{ CassandraTypes, Decoders, Encoders, UdtEncoding }
import io.getquill.util.ContextLogger
import io.getquill.util.Messages.fail

import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration._
import scala.util.Try

abstract class CassandraSessionContext[N <: NamingStrategy]
  extends CassandraPrepareContext[N]
  with CassandraBaseContext[N]

/**
 * When using this context, we cannot encode UDTs since does not have a proper CassandraSession trait mixed in with udtValueOf.
 * Certain contexts e.g. the CassandraLagomContext does not currently have this ability.
 */
abstract class CassandraSessionlessContext[N <: NamingStrategy]
  extends CassandraPrepareContext[N]

trait CassandraPrepareContext[N <: NamingStrategy] extends CassandraRowContext[N] with CassandraContext[N] {
  protected def prepareAsync(cql: String)(implicit executionContext: ExecutionContext): Future[BoundStatement]

  def probe(cql: String): Try[_] = {
    Try {
      Await.result(prepareAsync(cql)(ExecutionContext.Implicits.global), 1.minute)
      ()
    }
  }

  protected def prepareAsyncAndGetStatement(cql: String, prepare: Prepare, session: Session, logger: ContextLogger)(implicit executionContext: ExecutionContext): Future[BoundStatement] = {
    val prepareResult = this.prepareAsync(cql).map(row => prepare(row, session))
    val preparedRow = prepareResult.map {
      case (params, bs) =>
        logger.logQuery(cql, params)
        bs
    }
    preparedRow
  }

  def executeActionReturning[O](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[O], returningColumn: String)(info: ExecutionInfo, dc: Runner): Unit =
    fail("Cassandra doesn't support `returning`.")

  def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: Extractor[T])(info: ExecutionInfo, dc: Runner): Unit =
    fail("Cassandra doesn't support `returning`.")
}

trait CassandraBaseContext[N <: NamingStrategy] extends CassandraRowContext[N] {
  override type Session = CassandraSession
}

trait CassandraRowContext[N <: NamingStrategy]
  extends CassandraContext[N]
  with StandardContext[CqlIdiom, N]
  with Encoders
  with Decoders
  with CassandraTypes
  with UdtEncoding {

  val idiom = CqlIdiom

  override type PrepareRow = BoundStatement
  override type ResultRow = Row
  type Runner = Unit

  // Usually this is io.getquill.context.CassandraSession so you can use udtValueOf but not always e.g. for Lagom it is different
  type Session <: UdtValueLookup

  override type RunActionReturningResult[T] = Unit
  override type RunBatchActionReturningResult[T] = Unit
}

