package io.getquill.context.jasync

import com.github.jasync.sql.db.Connection

import scala.concurrent.ExecutionContext

case class TransactionalExecutionContext(ec: ExecutionContext, conn: Connection)
  extends ExecutionContext {

  def execute(runnable: Runnable): Unit =
    ec.execute(runnable)

  def reportFailure(cause: Throwable): Unit =
    ec.reportFailure(cause)
}
