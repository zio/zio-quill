package io.getquill.context.ndbc.postgres

import scala.concurrent.duration.Duration

import io.getquill.{ Literal, NdbcPostgresContext, TestEntities }
import io.getquill.context.sql.{ TestDecoders, TestEncoders }
import io.trane.future.scala.{ Await, Future }

class TestContext extends NdbcPostgresContext(Literal, "testPostgresDB")
  with TestEntities
  with TestEncoders
  with TestDecoders {

  def get[T](f: Future[T]): T = Await.result(f, Duration.Inf)
}