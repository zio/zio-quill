package io.getquill.sources

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration

import io.getquill.naming.Literal

package object cassandra {

  val mirrorSource = mirror.cassandraMirrorSource

  val testSyncDB = new CassandraSyncSource[Literal]("testSyncDB")

  val testAsyncDB = new CassandraAsyncSource[Literal]("testAsyncDB")

  val testStreamDB = new CassandraStreamSource[Literal]("testStreamDB")

  def await[T](f: Future[T]): T = Await.result(f, Duration.Inf)
}
