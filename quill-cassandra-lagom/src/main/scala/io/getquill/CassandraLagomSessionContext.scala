package io.getquill

import akka.Done
import com.datastax.driver.core.BoundStatement
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import io.getquill.context.BindMacro
import io.getquill.context.cassandra.CassandraSessionContext

import scala.concurrent.{ ExecutionContext, Future }
import scala.language.experimental.macros

abstract class CassandraLagomSessionContext[N <: NamingStrategy](
  val naming: N,
  session:    CassandraSession
)
  extends CassandraSessionContext[N] {

  override type RunActionResult = Done
  override type RunBatchActionResult = Done

  def bind[T](quoted: Quoted[T]): Future[PrepareRow] = macro BindMacro.bindQuery[T]

  override def prepareAsync(cql: String)(implicit executionContext: ExecutionContext): Future[BoundStatement] = {
    session.prepare(cql).map(_.bind())
  }

  override def close() = {
    import scala.concurrent.ExecutionContext.Implicits.global
    session.underlying().map(_.close())
    ()
  }

}

