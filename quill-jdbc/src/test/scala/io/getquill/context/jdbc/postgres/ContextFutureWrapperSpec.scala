package io.getquill.context.jdbc.postgres

import io.getquill.context.sql.EncodingSpec
import io.getquill.ContextFutureWrapper

class ContextFutureWrapperSpec extends EncodingSpec {
  val wrappedContext = new ContextFutureWrapper(testContext)
  import wrappedContext._
  implicit val ec = scala.concurrent.ExecutionContext.global

  "runs insert and query correctly and returns Futures" in {
    wrappedContext.run(delete)
    wrappedContext.run(liftQuery(insertValues).foreach(p => insert(p)))
    verify(testContext.run(query[EncodingTestEntity]))
  }
}
