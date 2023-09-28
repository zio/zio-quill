package io.getquill

import monix.execution.Scheduler

package object postgres {
  Scheduler.global
  object testContext extends PostgresMonixJdbcContext(Literal, "testPostgresDB") with TestEntities
}
