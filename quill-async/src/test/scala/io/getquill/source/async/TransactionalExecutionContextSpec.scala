package io.getquill.source.async

import io.getquill.Spec
import scala.concurrent.ExecutionContext

class TransactionalExecutionContextSpec extends Spec {

  "uses the wrapped context to execute runnables" in {
    var called = false
    val ec = new ExecutionContext {
      def execute(r: Runnable) = {
        called = true
        r.run
      }
      def reportFailure(t: Throwable) = ???
    }
    TransactionalExecutionContext(ec, null).execute {
      new Runnable {
        override def run = {}
      }
    }
    called mustEqual true
  }

  "uses the wrapped context to report errors" in {
	  var called = false
    val ec = new ExecutionContext {
      def execute(r: Runnable) = ???
      def reportFailure(t: Throwable) = called = true
    }
    TransactionalExecutionContext(ec, null).reportFailure(new IllegalStateException)
    called mustEqual true
  }
}