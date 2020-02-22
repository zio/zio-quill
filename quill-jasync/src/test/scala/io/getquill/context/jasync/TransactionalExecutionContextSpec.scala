package io.getquill.context.jasync

import io.getquill.Spec
import scala.concurrent.ExecutionContext
import scala.collection.mutable.ListBuffer

class TransactionalExecutionContextSpec extends Spec {

  "uses the wrapped context to execute runnables" in {
    val executed = ListBuffer[Runnable]()
    val ec = new ExecutionContext {
      def execute(r: Runnable) = {
        executed += r
        r.run
      }
      def reportFailure(t: Throwable) = ???
    }
    val runnable = new Runnable {
      override def run = {}
    }
    TransactionalExecutionContext(ec, null).execute(runnable)
    executed.result mustEqual List(runnable)
  }

  "uses the wrapped context to report errors" in {
    val reported = ListBuffer[Throwable]()
    val ec = new ExecutionContext {
      def execute(r: Runnable) = ???
      def reportFailure(t: Throwable) = {
        val r = reported += t
      }
    }
    val exception = new IllegalStateException
    TransactionalExecutionContext(ec, null).reportFailure(exception)
    reported.result mustEqual List(exception)
  }
}