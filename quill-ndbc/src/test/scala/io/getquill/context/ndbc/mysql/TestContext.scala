package io.getquill.context.ndbc.mysql

import io.getquill.context.sql.{ TestDecoders, TestEncoders }
import io.getquill.{ Literal, TestEntities }

import io.trane.future.scala.{ Await, Future }
import scala.concurrent.duration.Duration
import io.getquill.MysqlNdbcContext

class TestContext extends MysqlNdbcContext(Literal, "testPostgresDB")
  with TestEntities
  with TestEncoders
  with TestDecoders {

  def get[T](f: Future[T]): T = Await.result(f, Duration.Inf)
}
