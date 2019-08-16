package io.getquill.context

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration

import io.getquill._
import io.getquill.Literal

package object cassandra {

  lazy val mirrorContext = new CassandraMirrorContext(Literal) with CassandraTestEntities
  lazy val capsMirrorContext = new CassandraMirrorContext(UpperCaseNonDefault) with CassandraTestEntities

  lazy val testSyncDB = new CassandraSyncContext(Literal, "testSyncDB") with CassandraTestEntities

  lazy val testAsyncDB = new CassandraAsyncContext(Literal, "testAsyncDB") with CassandraTestEntities

  def await[T](f: Future[T]): T = Await.result(f, Duration.Inf)
}
