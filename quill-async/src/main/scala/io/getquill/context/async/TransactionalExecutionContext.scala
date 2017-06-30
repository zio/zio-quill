package io.getquill.context.async

import com.github.mauricio.async.db.Connection

import scala.concurrent.ExecutionContext

case class TransactionalExecutionContext(ec: ExecutionContext, conn: Connection)
  extends ExecutionContext {

  def execute(runnable: Runnable): Unit =
    ec.execute(runnable)

  def reportFailure(cause: Throwable): Unit =
    ec.reportFailure(cause)
}
