package io.getquill

import akka.Done
import com.datastax.driver.core.BoundStatement
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import io.getquill.context.cassandra.CassandraSessionContext

import scala.concurrent.{ ExecutionContext, Future }

abstract class CassandraLagomSessionContext[N <: NamingStrategy](
  val naming:  N,
  val session: CassandraSession
)
  extends CassandraSessionContext[N] {

  override type RunActionResult = Done
  override type RunBatchActionResult = Done
  override type Session = CassandraSession

  override def prepareAsync(cql: String)(implicit executionContext: ExecutionContext): Future[BoundStatement] = {
    session.prepare(cql).map(_.bind())
  }

  override def close() = {
    import scala.concurrent.ExecutionContext.Implicits.global
    session.underlying().map(_.close())
    ()
  }

}

