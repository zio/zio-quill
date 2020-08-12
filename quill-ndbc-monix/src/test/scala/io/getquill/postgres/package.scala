package io.getquill

import io.getquill.context.sql.{ TestDecoders, TestEncoders }
import monix.execution.Scheduler

package object postgres {
  private implicit val scheduler = Scheduler.global
  object testContext extends PostgresMonixNdbcContext(Literal, "testPostgresDB")
    with TestEntities with TestEncoders with TestDecoders
}
