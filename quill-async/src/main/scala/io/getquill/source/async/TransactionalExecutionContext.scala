package io.getquill.source.async

import scala.concurrent.ExecutionContext

import com.github.mauricio.async.db.Connection

case class TransactionalExecutionContext(ec: ExecutionContext, conn: Connection)
    extends ExecutionContext {

  def execute(runnable: Runnable): Unit =
    ec.execute(runnable)

  def reportFailure(cause: Throwable): Unit =
    ec.reportFailure(cause)
}
