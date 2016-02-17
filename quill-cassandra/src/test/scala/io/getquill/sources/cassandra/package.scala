package io.getquill.sources

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration

import io.getquill.naming.Literal
import io.getquill._

package object cassandra {

  val mirrorSource = source(new CassandraMirrorSourceConfig("test"))

  val testSyncDB = source(new CassandraSyncSourceConfig[Literal]("testSyncDB") with NoQueryProbing)

  val testAsyncDB = source(new CassandraAsyncSourceConfig[Literal]("testAsyncDB") with NoQueryProbing)

  val testStreamDB = source(new CassandraStreamSourceConfig[Literal]("testStreamDB") with NoQueryProbing)

  def await[T](f: Future[T]): T = Await.result(f, Duration.Inf)
}
