package io.getquill.context

import io.getquill._

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }

package object cassandra {

  lazy val mirrorContext = new CassandraMirrorContext with TestExtras

  lazy val testSyncDB = new CassandraSyncContext[Literal]("testSyncDB") with TestExtras

  lazy val testAsyncDB = new CassandraAsyncContext[Literal]("testAsyncDB") with TestExtras

  lazy val testStreamDB = new CassandraStreamContext[Literal]("testStreamDB") with TestExtras

  def await[T](f: Future[T]): T = Await.result(f, Duration.Inf)
}
