package io.getquill.context

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration

import io.getquill._
import io.getquill.Literal
import io.getquill.CassandraStreamContext

package object cassandra {

  lazy val mirrorContext = new CassandraMirrorContext with TestEntities

  lazy val testSyncDB = new CassandraSyncContext[Literal]("testSyncDB") with TestEntities

  lazy val testAsyncDB = new CassandraAsyncContext[Literal]("testAsyncDB") with TestEntities

  lazy val testStreamDB = new CassandraStreamContext[Literal]("testStreamDB") with TestEntities

  def await[T](f: Future[T]): T = Await.result(f, Duration.Inf)
}
