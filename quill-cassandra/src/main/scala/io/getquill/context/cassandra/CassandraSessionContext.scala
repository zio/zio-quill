package io.getquill.context.cassandra

import com.datastax.driver.core._
import io.getquill.NamingStrategy
import io.getquill.context.Context
import io.getquill.context.cassandra.encoding.{ CassandraTypes, Decoders, Encoders, UdtEncoding }
import io.getquill.util.ContextLogger
import io.getquill.util.Messages.fail

import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration._
import scala.util.Try

abstract class CassandraSessionContext[N <: NamingStrategy]
  extends Context[CqlIdiom, N]
  with CassandraContext[N]
  with Encoders
  with Decoders
  with CassandraTypes
  with UdtEncoding {

  val idiom = CqlIdiom

  override type PrepareRow = BoundStatement
  override type ResultRow = Row

  override type RunActionReturningResult[T] = Unit
  override type RunBatchActionReturningResult[T] = Unit

  protected def prepareAsync(cql: String)(implicit executionContext: ExecutionContext): Future[BoundStatement]

  def probe(cql: String): Try[_] = {
    Try {
      Await.result(prepareAsync(cql)(ExecutionContext.Implicits.global), 1.minute)
      ()
    }
  }

  protected def prepareAsyncAndGetStatement(cql: String, prepare: Prepare, logger: ContextLogger)(implicit executionContext: ExecutionContext): Future[BoundStatement] = {
    val prepareResult = this.prepareAsync(cql).map(prepare)
    val preparedRow = prepareResult.map {
      case (params, bs) =>
        logger.logQuery(cql, params)
        bs
    }
    preparedRow
  }

  def executeActionReturning[O](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[O], returningColumn: String): Unit =
    fail("Cassandra doesn't support `returning`.")

  def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: Extractor[T]): Unit =
    fail("Cassandra doesn't support `returning`.")
}

