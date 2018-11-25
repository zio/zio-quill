package io.getquill

import monix.execution.Scheduler

package object postgres {
  private implicit val scheduler = Scheduler.global
  object testContext extends PostgresMonixJdbcContext(Literal, "testPostgresDB") with TestEntities
}
