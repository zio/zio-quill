package io.getquill.context.async.postgres

import io.getquill.context.sql.{ TestDecoders, TestEncoders }
import io.getquill.{ Literal, PostgresAsyncContext, TestEntities }

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration

class TestContext extends PostgresAsyncContext[Literal]("testPostgresDB")
  with TestEntities
  with TestEncoders
  with TestDecoders {

  def await[T](f: Future[T]): T = Await.result(f, Duration.Inf)
}
