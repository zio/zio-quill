package io.getquill.sources.cassandra.util

import com.google.common.util.concurrent.FutureCallback
import com.datastax.driver.core.ResultSetFuture
import com.datastax.driver.core.ResultSet
import scala.concurrent.Promise
import scala.concurrent.Future
import com.google.common.util.concurrent.Futures
import language.implicitConversions

object FutureConversions {

  implicit def toScalaFuture(fut: ResultSetFuture): Future[ResultSet] = {
    val p = Promise[ResultSet]()
    Futures.addCallback(fut,
      new FutureCallback[ResultSet] {
        def onSuccess(r: ResultSet) = {
          p.success(r)
          ()
        }
        def onFailure(t: Throwable) = {
          p.failure(t)
          ()
        }
      })
    p.future
  }
}
