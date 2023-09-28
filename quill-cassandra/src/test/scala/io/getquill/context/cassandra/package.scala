package io.getquill.context

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration

import io.getquill._
import io.getquill.Literal

package object cassandra {

  lazy val mirrorContext: CassandraMirrorContext[Literal.type] with CassandraTestEntities     = new CassandraMirrorContext(Literal) with CassandraTestEntities
  lazy val capsMirrorContext: CassandraMirrorContext[UpperCaseNonDefault.type] with CassandraTestEntities = new CassandraMirrorContext(UpperCaseNonDefault) with CassandraTestEntities

  lazy val testSyncDB: CassandraSyncContext[Literal.type] with CassandraTestEntities = new CassandraSyncContext(Literal, "testSyncDB") with CassandraTestEntities

  lazy val testAsyncDB: CassandraAsyncContext[Literal.type] with CassandraTestEntities = new CassandraAsyncContext(Literal, "testAsyncDB") with CassandraTestEntities

  def await[T](f: Future[T]): T = Await.result(f, Duration.Inf)
}
