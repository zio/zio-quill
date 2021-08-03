package io.getquill

import akka.Done
import com.datastax.driver.core.BoundStatement
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import io.getquill.context.UdtValueLookup
import io.getquill.context.cassandra.CassandraSessionlessContext

import scala.concurrent.{ ExecutionContext, Future }

case class CassandraLagomSession(cs: CassandraSession) extends UdtValueLookup

abstract class CassandraLagomSessionContext[N <: NamingStrategy](
  val naming:  N,
  val session: CassandraSession
)
  extends CassandraSessionlessContext[N] {

  override type RunActionResult = Done
  override type RunBatchActionResult = Done
  override type Session = CassandraLagomSession

  val wrappedSession = CassandraLagomSession(session)

  override def prepareAsync(cql: String)(implicit executionContext: ExecutionContext): Future[BoundStatement] = {
    session.prepare(cql).map(_.bind())
  }

  override def close() = {
    import scala.concurrent.ExecutionContext.Implicits.global
    session.underlying().map(_.close())
    ()
  }

}

